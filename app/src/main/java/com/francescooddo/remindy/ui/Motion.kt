package com.francescooddo.remindy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import androidx.compose.animation.core.AnimationVector1D

object Motion {
    val press = tween<Float>(durationMillis = 130, easing = EaseOut)
    val toast = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
    val calendar = spring<Float>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
    val reveal = spring<Float>(dampingRatio = 0.92f, stiffness = Spring.StiffnessMedium)
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun rememberPulse(): Animatable<Float, AnimationVector1D> = remember { Animatable(1f) }
