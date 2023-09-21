package io.horizontalsystems.bankwallet.modules.pin.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.modules.pin.unlock.PinConfirmViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_lucian

@Composable
fun PinConfirm(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = viewModel<PinConfirmViewModel>(factory = PinConfirmViewModel.Factory())
    var showBiometricPrompt by remember { mutableStateOf(viewModel.uiState.fingerScannerEnabled) }
    var showBiometricDisabledAlert by remember { mutableStateOf(false) }

    if (viewModel.uiState.unlocked) {
        onSuccess.invoke()
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
            showCancelButton = true,
            showShakeAnimation = viewModel.uiState.showShakeAnimation,
            inputState = viewModel.uiState.inputState,
            onShakeAnimationFinish = { viewModel.onShakeAnimationFinish() },
            onCancelClick = onCancel
        )

        PinNumpad(
            onNumberClick = { number -> viewModel.onKeyClick(number) },
            onDeleteClick = { viewModel.onDelete() },
            showFingerScanner = viewModel.uiState.fingerScannerEnabled,
            showRandomizer = true,
            showBiometricPrompt = {
                showBiometricPrompt = true
            },
            inputState = viewModel.uiState.inputState
        )
    }
}