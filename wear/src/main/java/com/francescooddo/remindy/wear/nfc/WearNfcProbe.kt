package com.francescooddo.remindy.wear.nfc

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.francescooddo.remindy.wear.bridge.GooglePlayTagMessageGateway
import com.francescooddo.remindy.wear.bridge.WearTagCompletionSender
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WearNfcProbe private constructor(
    private val platform: ReaderModeGateway,
    private val operationIds: OperationIdFactory,
    private val mainThread: MainThread,
    private val observationSink: TagObservationSink,
    private val nowMillis: () -> Long,
    private val cleanup: () -> Unit,
) : DefaultLifecycleObserver {
    private val gate = SingleTagScanGate()
    private val mutableState = MutableStateFlow(NfcProbeState())
    private var foreground = false

    val state: StateFlow<NfcProbeState> = mutableState.asStateFlow()

    fun requestScan() {
        if (!foreground || mutableState.value.scan is ScanPhase.Starting || mutableState.value.scan is ScanPhase.Listening) {
            return
        }

        val facts = platform.inspect()
        mutableState.value = mutableState.value.copy(
            capability = NfcCapability.Inspected(
                featureNfcAdvertised = facts.featureNfcAdvertised,
                adapter = facts.adapter,
                readerMode = ReaderModeVerdict.NotTried,
            ),
        )

        when (facts.adapter) {
            AdapterAvailability.Missing -> {
                mutableState.value = mutableState.value.copy(scan = ScanPhase.Blocked(ScanBlocker.AdapterMissing))
                return
            }
            AdapterAvailability.Disabled -> {
                mutableState.value = mutableState.value.copy(scan = ScanPhase.Blocked(ScanBlocker.NfcDisabled))
                return
            }
            AdapterAvailability.Enabled -> Unit
        }

        val operationId = operationIds.newId()
        if (!gate.arm(operationId)) return
        mutableState.value = mutableState.value.copy(scan = ScanPhase.Starting(operationId))

        try {
            platform.enableReaderMode { tagId -> receiveTag(operationId, tagId) }
            mutableState.value = mutableState.value.copy(
                capability = NfcCapability.Inspected(
                    featureNfcAdvertised = facts.featureNfcAdvertised,
                    adapter = facts.adapter,
                    readerMode = ReaderModeVerdict.Accepted,
                ),
                scan = ScanPhase.Listening(operationId),
            )
        } catch (unsupported: UnsupportedOperationException) {
            gate.cancel()
            runCatching(platform::disableReaderMode)
            mutableState.value = mutableState.value.copy(
                capability = NfcCapability.Inspected(
                    featureNfcAdvertised = facts.featureNfcAdvertised,
                    adapter = facts.adapter,
                    readerMode = ReaderModeVerdict.Unsupported(unsupported.message),
                ),
                scan = ScanPhase.Blocked(ScanBlocker.ReaderModeUnsupported),
            )
        } catch (failure: RuntimeException) {
            gate.cancel()
            runCatching(platform::disableReaderMode)
            mutableState.value = mutableState.value.copy(
                capability = NfcCapability.Inspected(
                    featureNfcAdvertised = facts.featureNfcAdvertised,
                    adapter = facts.adapter,
                    readerMode = ReaderModeVerdict.Failed(failure.javaClass.simpleName),
                ),
                scan = ScanPhase.Failed("Reader mode failed"),
            )
        }
    }

    fun acceptDiscoveredUid(uid: String): Boolean {
        val tagUid = TagUid.fromHex(uid) ?: return false
        gate.cancel()
        runCatching(platform::disableReaderMode)
        val observation = TagObservation(
            operationId = operationIds.newId(),
            uid = tagUid,
            scannedAtMillis = nowMillis(),
        )
        observationSink.accept(observation)
        mutableState.value = mutableState.value.copy(
            scan = ScanPhase.Read(observation),
            lastObservation = observation,
        )
        return true
    }

    override fun onResume(owner: LifecycleOwner) {
        foreground = true
        requestScan()
    }

    override fun onPause(owner: LifecycleOwner) {
        stop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        cleanup()
    }

    private fun stop() {
        foreground = false
        gate.cancel()
        runCatching(platform::disableReaderMode)
        mutableState.value = mutableState.value.copy(scan = ScanPhase.Paused)
    }

    private fun receiveTag(operationId: ScanOperationId, tagId: ByteArray) {
        if (!gate.claim(operationId)) return
        val tagIdCopy = tagId.copyOf()
        mainThread.post {
            if (!gate.finish(operationId)) return@post
            val uid = TagUid.fromTagId(tagIdCopy)
            if (uid == null) {
                runCatching(platform::disableReaderMode)
                mutableState.value = mutableState.value.copy(scan = ScanPhase.Failed("Tag has no UID"))
                return@post
            }

            val observation = TagObservation(
                operationId = operationId,
                uid = uid,
                scannedAtMillis = nowMillis(),
            )
            runCatching(platform::disableReaderMode)
            observationSink.accept(observation)
            mutableState.value = mutableState.value.copy(
                scan = ScanPhase.Read(observation),
                lastObservation = observation,
            )
        }
    }

    companion object {
        fun attach(activity: ComponentActivity): WearNfcProbe {
            val handler = Handler(Looper.getMainLooper())
            val deliveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val sender = WearTagCompletionSender(GooglePlayTagMessageGateway(activity.applicationContext))
            val probe = WearNfcProbe(
                platform = AndroidReaderModeGateway(activity),
                operationIds = OperationIdFactory { ScanOperationId(UUID.randomUUID().toString()) },
                mainThread = MainThread { block ->
                    handler.post(block)
                    Unit
                },
                observationSink = TagObservationSink { observation ->
                    Log.d(
                        "RemindyWearNfc",
                        "tag_read operationId=${observation.operationId.value}",
                    )
                    deliveryScope.launch {
                        val delivery = sender.send(
                            operationId = observation.operationId.value,
                            uid = observation.uid.value,
                        )
                        Log.i(
                            "RemindyWearBridge",
                            "tag_delivery attempted=${delivery.attempted} delivered=${delivery.delivered}",
                        )
                    }
                    Unit
                },
                nowMillis = System::currentTimeMillis,
                cleanup = deliveryScope::cancel,
            )
            activity.lifecycle.addObserver(probe)
            return probe
        }

        internal fun createForTest(
            platform: ReaderModeGateway,
            operationIds: OperationIdFactory,
            mainThread: MainThread,
            observationSink: TagObservationSink = TagObservationSink.Ignore,
            nowMillis: () -> Long = System::currentTimeMillis,
        ): WearNfcProbe =
            WearNfcProbe(
                platform = platform,
                operationIds = operationIds,
                mainThread = mainThread,
                observationSink = observationSink,
                nowMillis = nowMillis,
                cleanup = {},
            )
    }
}
