package com.francescooddo.remindy.wear.nfc

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WearNfcProbeTest {
    @Test
    fun `enabled adapter is probed even when legacy feature is absent`() {
        val gateway = FakeGateway(featureAdvertised = false, adapter = AdapterAvailability.Enabled)
        val probe = WearNfcProbe.createForTest(
            platform = gateway,
            operationIds = FixedOperationIds(),
            mainThread = MainThread { it() },
        )

        probe.onResume(TestLifecycleOwner)

        assertEquals(1, gateway.enableCalls)
        assertIs<ScanPhase.Listening>(probe.state.value.scan)
    }

    @Test
    fun `held tag publishes one uppercase uid and stops polling`() {
        val gateway = FakeGateway(featureAdvertised = false, adapter = AdapterAvailability.Enabled)
        val observations = mutableListOf<TagObservation>()
        val probe = WearNfcProbe.createForTest(
            platform = gateway,
            operationIds = FixedOperationIds(),
            mainThread = MainThread { it() },
            observationSink = TagObservationSink(observations::add),
            nowMillis = { 1234L },
        )
        probe.onResume(TestLifecycleOwner)

        gateway.deliver(byteArrayOf(0x04, 0x00, 0xA1.toByte()))
        gateway.deliver(byteArrayOf(0x05, 0x06))

        assertEquals("0400A1", probe.state.value.lastObservation?.uid?.value)
        assertEquals(1234L, probe.state.value.lastObservation?.scannedAtMillis)
        assertEquals(1, observations.size)
        assertEquals(1, gateway.disableCalls)
        assertIs<ScanPhase.Read>(probe.state.value.scan)
    }

    @Test
    fun `unsupported reader mode is visible and does not crash`() {
        val gateway = FakeGateway(
            featureAdvertised = false,
            adapter = AdapterAvailability.Enabled,
            startFailure = UnsupportedOperationException("reader mode unavailable"),
        )
        val probe = WearNfcProbe.createForTest(
            platform = gateway,
            operationIds = FixedOperationIds(),
            mainThread = MainThread { it() },
        )

        probe.onResume(TestLifecycleOwner)

        val capability = assertIs<NfcCapability.Inspected>(probe.state.value.capability)
        assertIs<ReaderModeVerdict.Unsupported>(capability.readerMode)
        assertEquals(ScanBlocker.ReaderModeUnsupported, assertIs<ScanPhase.Blocked>(probe.state.value.scan).reason)
    }

    @Test
    fun `pause invalidates pending callback before delivery`() {
        val gateway = FakeGateway(featureAdvertised = true, adapter = AdapterAvailability.Enabled)
        val queued = mutableListOf<() -> Unit>()
        val probe = WearNfcProbe.createForTest(
            platform = gateway,
            operationIds = FixedOperationIds(),
            mainThread = MainThread(queued::add),
        )
        probe.onResume(TestLifecycleOwner)
        gateway.deliver(byteArrayOf(0x01))

        probe.onPause(TestLifecycleOwner)
        queued.single().invoke()

        assertEquals(null, probe.state.value.lastObservation)
        assertIs<ScanPhase.Paused>(probe.state.value.scan)
    }

    private object TestLifecycleOwner : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = error("The probe callbacks do not inspect the owner lifecycle")
    }

    private class FixedOperationIds : OperationIdFactory {
        private var next = 0

        override fun newId() = ScanOperationId("scan-${++next}")
    }

    private class FakeGateway(
        featureAdvertised: Boolean,
        adapter: AdapterAvailability,
        private val startFailure: RuntimeException? = null,
    ) : ReaderModeGateway {
        private val facts = PlatformNfcFacts(featureAdvertised, adapter)
        private var callback: ((ByteArray) -> Unit)? = null
        var enableCalls = 0
        var disableCalls = 0

        override fun inspect() = facts

        override fun enableReaderMode(onTagId: (ByteArray) -> Unit) {
            enableCalls += 1
            startFailure?.let { throw it }
            callback = onTagId
        }

        override fun disableReaderMode() {
            disableCalls += 1
        }

        fun deliver(bytes: ByteArray) {
            callback?.invoke(bytes)
        }
    }
}
