package io.horizontalsystems.bankwallet.modules.walletconnect.stellar

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.request.IWCAction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.MessageContent
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

class WCActionStellarSignXdr(
    private val params: String,
    private val peerName: String,
) : IWCAction {

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_Title)
    }

    @Composable
    override fun ScreenContent() {
        MessageContent(
            "Message",
            peerName,
            "Stellar",
            null,
        )
    }

    override fun performAction(): String {
        TODO("Not yet implemented")
    }
}
