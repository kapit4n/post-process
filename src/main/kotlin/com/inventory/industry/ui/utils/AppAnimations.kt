package com.inventory.industry.ui.utils

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp

object AppAnimations {
    const val HoverDurationMs: Int = 180
    const val FocusDurationMs: Int = 160
    const val FadeDurationMs: Int = 200

    fun <T> smoothTween(durationMillis: Int = HoverDurationMs): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis)

    fun <T> gentleSpring(): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
}

@Composable
fun <S> rememberAppTransition(
    targetState: S,
    label: String = "AppTransition",
): Transition<S> = updateTransition(targetState, label = label)

@Composable
fun animateElevationAsState(
    target: Dp,
    label: String = "elevation",
): State<Dp> =
    animateDpAsState(
        targetValue = target,
        animationSpec = AppAnimations.smoothTween(),
        label = label,
    )
