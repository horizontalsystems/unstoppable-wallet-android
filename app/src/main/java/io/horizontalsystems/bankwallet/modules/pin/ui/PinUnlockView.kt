package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinUnlockModule
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinUnlockViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_lucian

@Composable
fun PinUnlock(
    showCancelButton: Boolean,
    dismissWithSuccess: () -> Unit,
    onCancelClick: () -> Unit,
    viewModel: PinUnlockViewModel = viewModel(factory = PinUnlockModule.Factory(showCancelButton))
) {
    var showBiometricPrompt by remember { mutableStateOf(viewModel.uiState.fingerScannerEnabled) }
    var showBiometricDisabledAlert by remember { mutableStateOf(false) }

    if (viewModel.uiState.unlocked) {
        dismissWithSuccess.invoke()
        viewModel.unlocked()
    }

    if (viewModel.uiState.canceled) {
        onCancelClick.invoke()
        viewModel.canceled()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        PinTopBlock(
            modifier = Modifier.weight(1f),
            title = {
                val error = viewModel.uiState.error
                if (error != null) {
                    headline1_lucian(
                        text = error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    headline1_leah(
                        text = viewModel.uiState.title,
                        textAlign = TextAlign.Center
                    )
                }
            },
            enteredCount = viewModel.uiState.enteredCount,
            showCancelButton = viewModel.cancelButtonVisible,
            showShakeAnimation = viewModel.uiState.showShakeAnimation,
            inputState = viewModel.uiState.inputState,
            onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() },
            onCancelClick = { viewModel.onCancelClick() }
        )

        PinNumpad(
            onNumberClick = { number -> viewModel.onKeyClick(number) },
            onDeleteClick = { viewModel.onDelete() },
            showFingerScanner = viewModel.uiState.fingerScannerEnabled,
            showBiometricPrompt = {
                showBiometricPrompt = true
            },
            inputState = viewModel.uiState.inputState
        )
    }
}
