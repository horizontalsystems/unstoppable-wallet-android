package cash.p.terminal.modules.pin.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import cash.p.terminal.R
import cash.p.terminal.modules.pin.unlock.BiometricPromptUtils.createBiometricPrompt

@Composable
fun BiometricPromptDialog(
    onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
    onError: (Int) -> Unit
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(stringResource(R.string.BiometricAuth_DialogTitle))
        .setNegativeButtonText(stringResource(R.string.Button_Cancel))
        .setConfirmationRequired(false)
        .build()

    val context = LocalContext.current as? FragmentActivity

    val biometricPrompt = context?.let { createBiometricPrompt(it, onSuccess, onError) }!!
    LaunchedEffect(Unit) {
        biometricPrompt.authenticate(promptInfo)
    }
}
