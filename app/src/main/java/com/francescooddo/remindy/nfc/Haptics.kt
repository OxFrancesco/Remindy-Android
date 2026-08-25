package com.francescooddo.remindy.nfc

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {

    private fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun perform(context: Context, effect: VibrationEffect) {
        vibrator(context).vibrate(effect)
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray? = null): VibrationEffect =
        if (amplitudes != null) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }

    fun success(context: Context) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        } else {
            waveform(longArrayOf(0, 40))
        }
        perform(context, effect)
    }

    fun warning(context: Context) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        } else {
            waveform(longArrayOf(0, 30, 80, 30))
        }
        perform(context, effect)
    }

    fun error(context: Context) {
        perform(context, waveform(longArrayOf(0, 50, 60, 50, 60, 50)))
    }

    fun tick(context: Context) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            waveform(longArrayOf(0, 10))
        }
        perform(context, effect)
    }
}
