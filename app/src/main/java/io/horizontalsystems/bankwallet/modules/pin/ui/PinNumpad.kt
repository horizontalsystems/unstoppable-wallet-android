package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper


@Composable
fun PinNumpad(
    showFingerScanner: Boolean = false,
    onNumberClick: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    showBiometricPrompt: (() -> Unit)? = null,
    inputState: InputState = InputState.Enabled()
) {

    var numpadNumbers by remember { mutableStateOf(generateOriginalNumpadNumbers()) }
    var isRandomized by remember { mutableStateOf(false) }
    val enabled = inputState is InputState.Enabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberKey(numpadNumbers[0], enabled) { number -> onNumberClick(number) }
            NumberKey(numpadNumbers[1], enabled) { number -> onNumberClick(number) }
            NumberKey(numpadNumbers[2], enabled) { number -> onNumberClick(number) }
        }
        VSpacer(16.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberKey(numpadNumbers[3], enabled) { number -> onNumberClick(number) }
            NumberKey(numpadNumbers[4], enabled) { number -> onNumberClick(number) }
            NumberKey(numpadNumbers[5], enabled) { number -> onNumberClick(number) }
        }
        VSpacer(16.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberKey(numpadNumbers[6], enabled) { number -> onNumberClick(number) }
            NumberKey(numpadNumbers[7], enabled) { number -> onNumberClick(number) }
            NumberKey(numpadNumbers[8], enabled) { number -> onNumberClick(number) }
        }
        VSpacer(16.dp)
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
            NumberKey(numpadNumbers[9], enabled) { number -> onNumberClick(number) }
            ImageKey(
                image = R.drawable.ic_backspace,
                contentDescription = stringResource(R.string.Button_Delete),
                visible = true,
                enabled = enabled
            ) { onDeleteClick.invoke() }
        }
        VSpacer(24.dp)
        ButtonSecondaryDefault(
            title = if (isRandomized) {
                stringResource(R.string.Unlock_Regular)
            } else {
                stringResource(R.string.Unlock_Random)
            },
            onClick = {
                numpadNumbers = if (isRandomized) {
                    generateOriginalNumpadNumbers()
                } else {
                    generateRandomNumpadNumbers()
                }
                isRandomized = !isRandomized
            },
            enabled = enabled
        )
        VSpacer(48.dp)
    }

}

private fun generateOriginalNumpadNumbers(): List<Int> {
    return listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
}

private fun generateRandomNumpadNumbers(): List<Int> {
    return listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0).shuffled()
}

@Composable
private fun NumberKey(
    number: Int,
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
                tint = if (enabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.steel20,
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
