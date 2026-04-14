package com.quantum.wallet.bankwallet.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import com.quantum.wallet.core.helpers.HudHelper

@Composable
fun SnackbarError(errorMessage: String) {
    HudHelper.showErrorMessage(LocalView.current, errorMessage)
}
