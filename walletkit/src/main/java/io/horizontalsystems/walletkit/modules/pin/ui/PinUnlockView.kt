package io.horizontalsystems.walletkit.modules.pin.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.pin.unlock.PinUnlockModule
import io.horizontalsystems.walletkit.modules.pin.unlock.PinUnlockViewModel

@Composable
fun PinUnlock(isLocked: Boolean, modifier: Modifier = Modifier) {
    val viewModel = viewModel<PinUnlockViewModel>(factory = PinUnlockModule.Factory())
    val uiState = viewModel.uiState

    var showBiometricPrompt by remember { mutableStateOf(false) }
    var showBiometricDisabledAlert by remember { mutableStateOf(false) }
    LaunchedEffect(isLocked, uiState.biometricEnabled, uiState.inputState) {
        if (isLocked && uiState.biometricEnabled && uiState.inputState is PinUnlockModule.InputState.Enabled) {
            showBiometricPrompt = true
        }
    }

    if (uiState.unlocked) {
        viewModel.unlocked()
    }

    if (showBiometricPrompt) {
        BiometricPromptDialog(
            onSuccess = {
                viewModel.onBiometricsUnlock()
                showBiometricPrompt = false
            },
            onError = { errorCode ->
                if (errorCode == BiometricPrompt.ERROR_LOCKOUT) {
                    showBiometricDisabledAlert = true
                }
                showBiometricPrompt = false
            }
        )
    }

    if (showBiometricDisabledAlert) {
        BiometricDisabledDialog {
            showBiometricDisabledAlert = false
        }
    }

    PinUnlockScreen(
        viewModel = viewModel,
        onBiometricClick = { showBiometricPrompt = true },
        modifier = modifier,
    )
}

@Composable
private fun PinUnlockScreen(
    viewModel: PinUnlockViewModel,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState

    PinScreen(
        modifier = modifier,
        onBack = null,
        topBlock = {
            PinTopBlock(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Unlock_EnterPasscode),
                enteredCount = uiState.enteredCount,
                showShakeAnimation = uiState.showShakeAnimation,
                inputState = uiState.inputState,
                onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() },
            )
        },
        numpad = {
            PinNumpad(
                onNumberClick = { number -> viewModel.onKeyClick(number) },
                onDeleteClick = { viewModel.onDelete() },
                showBiometricScanner = uiState.biometricEnabled,
                pinRandomized = viewModel.pinRandomized,
                deleteEnabled = uiState.enteredCount > 0,
                showBiometricPrompt = onBiometricClick,
                inputState = uiState.inputState,
            )
        },
        bottomButton = {
            PinRandomButton(
                pinRandomized = viewModel.pinRandomized,
                enabled = uiState.inputState is PinUnlockModule.InputState.Enabled,
                onClick = { randomized -> viewModel.updatePinRandomized(randomized) },
            )
        },
    )
}
