package com.francescooddo.remindy.wear.nfc

import android.app.Activity
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import java.lang.reflect.InvocationTargetException
import org.lsposed.hiddenapibypass.HiddenApiBypass

internal class AndroidReaderModeGateway(
    private val activity: Activity,
) : ReaderModeGateway {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    override fun inspect(): PlatformNfcFacts {
        val availability = when {
            adapter == null -> AdapterAvailability.Missing
            !adapter.isEnabled -> AdapterAvailability.Disabled
            else -> AdapterAvailability.Enabled
        }
        return PlatformNfcFacts(
            featureNfcAdvertised = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            adapter = availability,
        )
    }

    override fun enableReaderMode(onTagId: (ByteArray) -> Unit) {
        val availableAdapter = adapter ?: throw IllegalStateException("NFC adapter is unavailable")
        val callback = NfcAdapter.ReaderCallback { tag -> onTagId(tag.id.copyOf()) }
        val extras = Bundle()
        try {
            availableAdapter.enableReaderMode(activity, callback, ReaderModePolicy.flags(), extras)
        } catch (unsupported: UnsupportedOperationException) {
            HiddenReaderMode.enable(
                adapter = availableAdapter,
                activity = activity,
                callback = callback,
                flags = ReaderModePolicy.flags(),
                extras = extras,
            )
            Log.w(LOG_TAG, "Public reader mode rejected; hidden activity manager accepted the request")
        }
    }

    override fun disableReaderMode() {
        val availableAdapter = adapter ?: return
        try {
            availableAdapter.disableReaderMode(activity)
        } catch (unsupported: UnsupportedOperationException) {
            HiddenReaderMode.disable(availableAdapter, activity)
        }
    }

    private companion object {
        const val LOG_TAG = "RemindyWearNfc"
    }
}

private object HiddenReaderMode {
    private val exemptionGranted by lazy {
        HiddenApiBypass.setHiddenApiExemptions("Landroid/nfc/")
    }

    fun enable(
        adapter: NfcAdapter,
        activity: Activity,
        callback: NfcAdapter.ReaderCallback,
        flags: Int,
        extras: Bundle,
    ) {
        invoke(
            adapter = adapter,
            methodName = "enableReaderMode",
            arguments = arrayOf(activity, callback, flags, extras),
        )
    }

    fun disable(adapter: NfcAdapter, activity: Activity) {
        invoke(
            adapter = adapter,
            methodName = "disableReaderMode",
            arguments = arrayOf(activity),
        )
    }

    private fun invoke(
        adapter: NfcAdapter,
        methodName: String,
        arguments: Array<out Any>,
    ) {
        if (!exemptionGranted) {
            throw UnsupportedOperationException("Hidden NFC API exemption failed")
        }
        try {
            val managerField = NfcAdapter::class.java.getDeclaredField("mNfcActivityManager")
            managerField.isAccessible = true
            val manager = managerField.get(adapter)
                ?: throw UnsupportedOperationException("NFC activity manager is unavailable")
            val method = manager.javaClass.declaredMethods.singleOrNull { candidate ->
                candidate.name == methodName && candidate.parameterTypes.size == arguments.size
            } ?: throw UnsupportedOperationException("Hidden $methodName is unavailable")
            method.isAccessible = true
            method.invoke(manager, *arguments)
        } catch (invocation: InvocationTargetException) {
            val cause = invocation.targetException
            if (cause is RuntimeException) throw cause
            throw UnsupportedOperationException("Hidden $methodName failed", cause)
        } catch (failure: ReflectiveOperationException) {
            throw UnsupportedOperationException("Hidden $methodName is inaccessible", failure)
        } catch (failure: SecurityException) {
            throw UnsupportedOperationException("Hidden $methodName is blocked", failure)
        }
    }
}

internal object ReaderModePolicy {
    fun flags(): Int =
        NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
}
