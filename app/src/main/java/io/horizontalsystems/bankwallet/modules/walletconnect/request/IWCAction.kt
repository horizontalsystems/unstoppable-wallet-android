package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

interface IWCAction {
    fun getTitle(): TranslatableString

    @Composable
    fun ScreenContent()
    fun performAction(): String
}
