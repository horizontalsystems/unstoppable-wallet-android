package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

@Composable
fun PinTopBlock(
    modifier: Modifier = Modifier,
    title: @Composable ColumnScope.() -> Unit,
    enteredCount: Int,
    showCancelButton: Boolean = false,
    showShakeAnimation: Boolean = false,
    inputState: InputState = InputState.Enabled,
    onShakeAnimationFinish: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (inputState) {
            InputState.Enabled -> {
                title()
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.shake(
                        enabled = showShakeAnimation,
                        onAnimationFinish = { onShakeAnimationFinish?.invoke() }
                    )
                ) {
                    for (i in 1..PinModule.PIN_COUNT) {
                        IndicatorCircle(i <= enteredCount)
                    }
                }
                if (showCancelButton) {
                    Spacer(Modifier.height(24.dp))
                    ButtonSecondaryTransparent(
                        title = stringResource(R.string.Button_Cancel),
                        onClick = { onCancelClick?.invoke() }
                    )
                }
            }
            is InputState.Locked -> {
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
                title = { headline1_leah(text = "text") },
                enteredCount = 3,
                showCancelButton = true,
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
                title = { headline1_leah(text = "text") },
                enteredCount = 3,
                showCancelButton = true,
                showShakeAnimation = false,
                inputState = InputState.Locked("12:33")
            )
        }
    }
}
