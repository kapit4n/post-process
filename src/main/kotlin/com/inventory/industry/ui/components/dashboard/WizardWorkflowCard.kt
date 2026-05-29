package com.inventory.industry.ui.components.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.inventory.industry.ui.components.buttons.AppButton
import com.inventory.industry.ui.components.buttons.AppOutlinedButton
import com.inventory.industry.ui.components.cards.SectionCard
import com.inventory.industry.ui.theme.AppShapes
import com.inventory.industry.ui.theme.AppSpacing

@Composable
fun WizardWorkflowCard(
    title: String,
    steps: List<String>,
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    contentLabel: String = "wizardStep",
    content: @Composable (step: Int) -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = title,
        subtitle = steps[currentStep],
    ) {
        WizardStepper(steps = steps, currentStep = currentStep, onStepClick = onStepClick)
        LinearProgressIndicator(
            progress = { (currentStep + 1) / steps.size.toFloat() },
            modifier = Modifier.fillMaxWidth().clip(AppShapes.small),
        )
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState >= initialState) {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 6 })
                } else {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 6 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 6 })
                }
            },
            label = contentLabel,
        ) { step ->
            Box(modifier = Modifier.fillMaxWidth()) { content(step) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            AppOutlinedButton(
                text = "Atrás",
                onClick = onBack,
                enabled = currentStep > 0,
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            if (currentStep < steps.lastIndex) {
                AppButton(
                    text = "Siguiente",
                    onClick = onNext,
                    trailingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}
