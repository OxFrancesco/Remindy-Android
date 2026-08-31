package com.francescooddo.remindy.wear.nfc

@JvmInline
internal value class TagUid private constructor(val value: String) {
    companion object {
        fun fromTagId(bytes: ByteArray): TagUid? {
            if (bytes.isEmpty()) return null
            return TagUid(bytes.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) })
        }
    }
}

@JvmInline
internal value class ScanOperationId(val value: String)

internal data class TagObservation(
    val operationId: ScanOperationId,
    val uid: TagUid,
    val scannedAtMillis: Long,
)

internal enum class AdapterAvailability {
    Missing,
    Disabled,
    Enabled,
}

internal sealed interface ReaderModeVerdict {
    data object NotTried : ReaderModeVerdict
    data object Accepted : ReaderModeVerdict
    data class Unsupported(val reason: String?) : ReaderModeVerdict
    data class Failed(val reason: String) : ReaderModeVerdict
}

internal sealed interface NfcCapability {
    data object Checking : NfcCapability

    data class Inspected(
        val featureNfcAdvertised: Boolean,
        val adapter: AdapterAvailability,
        val readerMode: ReaderModeVerdict,
    ) : NfcCapability
}

internal enum class ScanBlocker {
    AdapterMissing,
    NfcDisabled,
    ReaderModeUnsupported,
}

internal sealed interface ScanPhase {
    data object Paused : ScanPhase
    data class Starting(val operationId: ScanOperationId) : ScanPhase
    data class Listening(val operationId: ScanOperationId) : ScanPhase
    data class Read(val observation: TagObservation) : ScanPhase
    data class Blocked(val reason: ScanBlocker) : ScanPhase
    data class Failed(val message: String) : ScanPhase
}

internal data class NfcProbeState(
    val capability: NfcCapability = NfcCapability.Checking,
    val scan: ScanPhase = ScanPhase.Paused,
    val lastObservation: TagObservation? = null,
)

internal data class PlatformNfcFacts(
    val featureNfcAdvertised: Boolean,
    val adapter: AdapterAvailability,
)

internal fun interface MainThread {
    fun post(block: () -> Unit)
}

internal fun interface OperationIdFactory {
    fun newId(): ScanOperationId
}

internal fun interface TagObservationSink {
    fun accept(observation: TagObservation)

    companion object {
        val Ignore = TagObservationSink { }
    }
}

internal interface ReaderModeGateway {
    fun inspect(): PlatformNfcFacts

    @Throws(UnsupportedOperationException::class)
    fun enableReaderMode(onTagId: (ByteArray) -> Unit)

    fun disableReaderMode()
}
