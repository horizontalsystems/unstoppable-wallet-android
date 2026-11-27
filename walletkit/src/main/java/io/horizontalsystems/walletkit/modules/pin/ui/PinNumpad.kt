package io.horizontalsystems.walletkit.modules.pin.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonSize
import io.horizontalsystems.designkit.components.ButtonStyle
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.pin.unlock.PinUnlockModule.InputState
import io.horizontalsystems.walletkit.uiv3.TestTags
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance

/**
 * The passcode keypad: three columns across the full width, the digits in
 * four rows, the biometric key bottom left and the delete key bottom right.
 *
 * @param pinRandomized true shuffles the digits; they are shuffled again
 * whenever the value changes.
 * @param deleteEnabled false while there is nothing to delete.
 */
@Composable
fun PinNumpad(
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBiometricScanner: Boolean = false,
    pinRandomized: Boolean? = null,
    deleteEnabled: Boolean = true,
    showBiometricPrompt: (() -> Unit)? = null,
    inputState: InputState = InputState.Enabled(),
) {
    val numpadNumbers = remember(pinRandomized) { getNumpadNumbers(pinRandomized) }
    val enabled = inputState is InputState.Enabled

    Column(modifier = modifier.fillMaxWidth()) {
        numpadNumbers.dropLast(1).chunked(NUMPAD_COLUMNS).forEach { row ->
            Row {
                row.forEach { number ->
                    NumberKey(number, enabled, Modifier.weight(1f)) { onNumberClick(number) }
                }
            }
        }

        Row {
            if (showBiometricScanner) {
                IconKey(
                    icon = R.drawable.touchid_24,
                    contentDescription = stringResource(R.string.Unlock_BiometricScanner),
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.PIN_KEY_BIOMETRIC),
                ) {
                    showBiometricPrompt?.invoke()
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            val lastNumber = numpadNumbers.last()
            NumberKey(lastNumber, enabled, Modifier.weight(1f)) { onNumberClick(lastNumber) }
            IconKey(
                icon = R.drawable.arrow_m_left_24,
                contentDescription = stringResource(R.string.Button_Delete),
                enabled = enabled && deleteEnabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.PIN_KEY_DELETE),
                onClick = onDeleteClick,
            )
        }
    }
}

/**
 * The switch under the keypad that shuffles its digits: outlined while the
 * digits are in order, filled while they are shuffled.
 */
@Composable
fun PinRandomButton(
    pinRandomized: Boolean,
    enabled: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HSButton(
        modifier = modifier.testTag(TestTags.PIN_RANDOM),
        variant = ButtonVariant.Secondary,
        style = if (pinRandomized) ButtonStyle.Solid else ButtonStyle.Transparent,
        size = ButtonSize.Small,
        title = stringResource(R.string.Unlock_Random),
        enabled = enabled,
        onClick = { onClick(!pinRandomized) },
    )
}

private fun getNumpadNumbers(pinRandomized: Boolean?): List<Int> {
    val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    return if (pinRandomized == true) numbers.shuffled() else numbers
}

@Composable
private fun NumberKey(
    number: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.andy
    val style = ComposeAppTheme.typography.title2R

    NumpadButton(
        enabled = enabled,
        pressedScale = TEXT_PRESSED_SCALE,
        onClick = onClick,
        modifier = modifier.testTag(TestTags.pinKey(number)),
        // A pressed digit is also heavier
        pressedContent = {
            Text(
                text = number.toString(),
                style = style.copy(fontWeight = FontWeight.Medium),
                color = color,
            )
        },
    ) {
        Text(text = number.toString(), style = style, color = color)
    }
}

@Composable
private fun IconKey(
    icon: Int,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NumpadButton(
        enabled = enabled,
        pressedScale = ICON_PRESSED_SCALE,
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(icon),
            tint = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.andy,
            contentDescription = contentDescription,
        )
    }
}

/**
 * One key of the keypad. A press has no ripple: the content grows to
 * [pressedScale] while the key is held and shrinks back on release. A
 * release that comes before the content has grown shows it at full size at
 * once and shrinks it from there, as the Figma prototype plays a quick
 * click, so a short tap is as visible as a long press and takes no longer
 * than the way back.
 *
 * @param pressedContent what the content fades into while it grows, when
 * the pressed key looks different beyond its size.
 */
@Composable
private fun NumpadButton(
    enabled: Boolean,
    pressedScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressedContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val press = remember { Animatable(0f) }

    LaunchedEffect(interactionSource) {
        val spec = tween<Float>(durationMillis = PRESS_ANIMATION_MILLIS, easing = EaseOut)
        // The latest press interaction only, so the release cuts the growth
        // short. Hover and focus interactions are left out: one arriving
        // mid-animation would cancel it and leave the key enlarged.
        interactionSource.interactions.filterIsInstance<PressInteraction>().collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> press.animateTo(1f, spec)
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    press.snapTo(1f)
                    press.animateTo(0f, spec)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    HudHelper.vibrate(context)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Scaled and faded while drawing, so a growing key does not move
        // its neighbours and a frame costs no recomposition
        Box(
            modifier = Modifier.graphicsLayer {
                val scale = 1f + (pressedScale - 1f) * press.value
                scaleX = scale
                scaleY = scale
            },
            contentAlignment = Alignment.Center,
        ) {
            val fades = pressedContent != null
            Box(Modifier.graphicsLayer { alpha = if (fades) 1f - press.value else 1f }) { content() }
            if (pressedContent != null) {
                // The same key drawn a second time: kept out of semantics, or
                // a screen reader would announce the digit twice
                Box(
                    Modifier
                        .graphicsLayer { alpha = press.value }
                        .clearAndSetSemantics { }
                ) {
                    pressedContent()
                }
            }
        }
    }
}

private const val NUMPAD_COLUMNS = 3

// A pressed digit grows from 36 to 48, a pressed icon from 24 to 36
private const val TEXT_PRESSED_SCALE = 48f / 36f
private const val ICON_PRESSED_SCALE = 36f / 24f
private const val PRESS_ANIMATION_MILLIS = 300

@Preview
@Composable
private fun Preview_Pin() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PinNumpad(
                onNumberClick = { },
                onDeleteClick = { },
                showBiometricScanner = true,
                pinRandomized = true,
            )
            PinRandomButton(pinRandomized = true, enabled = true, onClick = { })
        }
    }
}

@Preview
@Composable
private fun Preview_PinLocked() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PinNumpad(
                onNumberClick = { },
                onDeleteClick = { },
                showBiometricScanner = true,
                pinRandomized = false,
                inputState = InputState.Locked("12:33"),
            )
            PinRandomButton(pinRandomized = false, enabled = false, onClick = { })
        }
    }
}
