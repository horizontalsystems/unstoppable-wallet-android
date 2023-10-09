package io.horizontalsystems.bankwallet.modules.pin.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinUnlockModule.InputState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.animations.shake
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian

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
                    subhead2_grey(text = title)
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
    val color = if (active) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.steel20
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
    ComposeAppTheme {
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
    ComposeAppTheme {
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
