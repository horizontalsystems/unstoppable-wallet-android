package cash.p.terminal.modules.pin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.pin.PinModule
import cash.p.terminal.modules.pin.unlock.PinUnlockModule.InputState
import cash.p.terminal.ui.compose.animations.shake
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_jacob
import cash.p.terminal.ui_compose.components.subhead2_lucian
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun PinTopBlock(
    modifier: Modifier = Modifier,
    title: String,
    error: String? = null,
    enteredCount: Int,
    showShakeAnimation: Boolean = false,
    inputState: InputState = InputState.Enabled(),
    onShakeAnimationFinish: (() -> Unit)? = null
) {
    when (inputState) {
        is InputState.Enabled -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    subhead2_grey(
                        text = title,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.shake(
                        enabled = showShakeAnimation,
                        onAnimationFinish = { onShakeAnimationFinish?.invoke() }
                    )
                ) {
                    repeat(PinModule.PIN_COUNT) {
                        IndicatorCircle(it < enteredCount)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Spacer(Modifier.height(16.dp))
                    when {
                        error != null -> {
                            subhead2_lucian(text = error)
                        }
                        inputState.attemptsLeft != null -> {
                            subhead2_jacob(
                                text = stringResource(R.string.Unlock_AttemptsLeft, inputState.attemptsLeft)
                            )
                        }
                    }
                }
            }
        }
        is InputState.Locked -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_lock_48),
                    contentDescription = null
                )
                Spacer(Modifier.height(16.dp))
                subhead2_grey(
                    text = stringResource(
                        R.string.Unlock_WalletDisabledUntil,
                        inputState.until
                    )
                )
            }
        }
    }
}

@Composable
private fun IndicatorCircle(active: Boolean) {
    val color = if (active) cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.jacob else cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.steel20
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Preview
@Composable
fun Preview_PinTopBlockEnabled() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            PinTopBlock(
                title = "text",
                enteredCount = 3,
                showShakeAnimation = false,
            )
        }
    }
}

@Preview
@Composable
fun Preview_PinTopBlockLocked() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            PinTopBlock(
                title = "text",
                enteredCount = 3,
                showShakeAnimation = false,
                inputState = InputState.Locked("12:33")
            )
        }
    }
}
