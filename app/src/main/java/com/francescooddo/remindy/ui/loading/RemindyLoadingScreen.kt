package com.francescooddo.remindy.ui.loading

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.francescooddo.remindy.ui.AppViewModel
import com.francescooddo.remindy.ui.rememberReducedMotion
import com.francescooddo.remindy.ui.tasks.TasksScreen
import kotlinx.coroutines.delay

private const val LoadingDurationMillis = 1_000
private val LoadingBackground = Color(0xFF4F46E5)

@Composable
fun RemindyLoadingGate(viewModel: AppViewModel) {
    var loading by rememberSaveable { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        TasksScreen(viewModel)
        if (loading) {
            RemindyLoadingScreen(onFinished = { loading = false })
        }
    }
}

@Composable
private fun RemindyLoadingScreen(onFinished: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    val window = checkNotNull(LocalActivity.current).window
    val view = LocalView.current

    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        val lightStatusBars = controller.isAppearanceLightStatusBars
        val lightNavigationBars = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = lightStatusBars
            controller.isAppearanceLightNavigationBars = lightNavigationBars
        }
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(LoadingDurationMillis.toLong())
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LoadingDurationMillis,
                    easing = LinearEasing
                )
            )
        }
        currentOnFinished()
    }

    val frame = loadingFrame(if (reducedMotion) 0.5f else progress.value)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoadingBackground)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .semantics { contentDescription = "Loading Remindy" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(132.dp)) {
            val unit = size.minDimension / 108f
            val stroke = 7f * unit
            val white = Color.White

            drawCircle(
                color = white.copy(alpha = frame.dotAlpha),
                radius = 6f * unit * frame.dotScale,
                center = Offset(30f * unit, 54f * unit)
            )

            drawWave(
                center = Offset(22.1f * unit, 54f * unit),
                radius = 24f * unit,
                startAngle = -41.8f,
                sweepAngle = 83.6f,
                strokeWidth = stroke,
                color = white.copy(alpha = frame.innerAlpha)
            )
            drawWave(
                center = Offset(27.1f * unit, 54f * unit),
                radius = 36f * unit,
                startAngle = -46.2f,
                sweepAngle = 92.4f,
                strokeWidth = stroke,
                color = white.copy(alpha = frame.middleAlpha)
            )
            drawWave(
                center = Offset(32.3f * unit, 54f * unit),
                radius = 48f * unit,
                startAngle = -48.6f,
                sweepAngle = 97.2f,
                strokeWidth = stroke,
                color = white.copy(alpha = frame.outerAlpha)
            )
        }
    }
}

private fun DrawScope.drawWave(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    strokeWidth: Float,
    color: Color
) {
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

internal data class LoadingFrame(
    val dotScale: Float,
    val dotAlpha: Float,
    val innerAlpha: Float,
    val middleAlpha: Float,
    val outerAlpha: Float
)

internal fun loadingFrame(progress: Float): LoadingFrame {
    val value = progress.coerceIn(0f, 1f)
    return LoadingFrame(
        dotScale = interpolate(
            value,
            0f to 0.72f,
            0.3f to 1.2f,
            0.62f to 0.72f,
            1f to 0.42f
        ),
        dotAlpha = interpolate(value, 0f to 0.88f, 0.16f to 1f, 0.8f to 1f, 1f to 0.9f),
        innerAlpha = interpolate(value, 0f to 0.72f, 0.18f to 1f, 0.76f to 1f, 1f to 0.08f),
        middleAlpha = interpolate(value, 0f to 0.5f, 0.28f to 1f, 0.84f to 1f, 1f to 0.48f),
        outerAlpha = interpolate(value, 0f to 0.28f, 0.36f to 1f, 1f to 1f)
    )
}

private fun interpolate(progress: Float, vararg keyframes: Pair<Float, Float>): Float {
    val upperIndex = keyframes.indexOfFirst { (position) -> position >= progress }
    if (upperIndex <= 0) return keyframes.first().second
    if (upperIndex == -1) return keyframes.last().second

    val lower = keyframes[upperIndex - 1]
    val upper = keyframes[upperIndex]
    val localProgress = (progress - lower.first) / (upper.first - lower.first)
    return lower.second + (upper.second - lower.second) * localProgress
}
