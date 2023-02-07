package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinUnlockModule.InputState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper


@Composable
fun PinNumpad(
    showFingerScanner: Boolean = false,
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    showBiometricPrompt: (() -> Unit)? = null,
    inputState: InputState = InputState.Enabled
) {

    val enabled = inputState == InputState.Enabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberKey(1, "", enabled) { number -> onNumberClick(number) }
            NumberKey(2, "abc", enabled) { number -> onNumberClick(number) }
            NumberKey(3, "def", enabled) { number -> onNumberClick(number) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberKey(4, "ghi", enabled) { number -> onNumberClick(number) }
            NumberKey(5, "jkl", enabled) { number -> onNumberClick(number) }
            NumberKey(6, "mno", enabled) { number -> onNumberClick(number) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberKey(7, "pqrs", enabled) { number -> onNumberClick(number) }
            NumberKey(8, "tuv", enabled) { number -> onNumberClick(number) }
            NumberKey(9, "wxyz", enabled) { number -> onNumberClick(number) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ImageKey(
                image = R.drawable.icon_touch_id_24,
                contentDescription = stringResource(R.string.Unlock_BiometricScanner),
                visible = showFingerScanner,
                enabled = enabled
            ) {
                showBiometricPrompt?.invoke()
            }
            NumberKey(0, null, enabled) { number -> onNumberClick(number) }
            ImageKey(
                image = R.drawable.ic_backspace,
                contentDescription = stringResource(R.string.Button_Delete),
                visible = true,
                enabled = enabled
            ) { onDeleteClick.invoke() }
        }
        Spacer(Modifier.height(56.dp))
    }

}

@Composable
private fun NumberKey(
    number: Int,
    letters: String?,
    enabled: Boolean,
    onClick: (Int) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(1.dp, ComposeAppTheme.colors.steel20, CircleShape)
            .clickable(
                enabled = enabled,
                onClick = {
                    HudHelper.vibrate(context)
                    onClick.invoke(number)
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = number.toString(),
                style = ComposeAppTheme.typography.title2R,
                color = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.steel20,
            )
            letters?.let {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = it.uppercase(),
                    style = ComposeAppTheme.typography.micro,
                    color = if (enabled) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.steel20,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ImageKey(
    image: Int,
    contentDescription: String? = null,
    visible: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                enabled = visible && enabled,
                onClick = {
                    HudHelper.vibrate(context)
                    onClick.invoke()
                }
            )
    ) {
        if (visible) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(image),
                tint = ComposeAppTheme.colors.grey,
                contentDescription = contentDescription,
            )
        }
    }
}

@Preview
@Composable
fun Preview_Pin() {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            PinNumpad(
                onNumberClick = { },
                onDeleteClick = { },
                showFingerScanner = true,
                showBiometricPrompt = {

                }
            )
        }
    }
}

@Preview
@Composable
fun Preview_PinLocked() {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            PinNumpad(
                onNumberClick = { },
                onDeleteClick = { },
                showFingerScanner = true,
                showBiometricPrompt = {},
                inputState = InputState.Locked("12:33")
            )
        }
    }
}
