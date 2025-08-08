package io.horizontalsystems.bankwallet.modules.walletconnect.request

import android.util.Log
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

class WalletConnectActionStellarSignAndSubmitXdr(
    private val params: String,
    private val peerName: String,
) : IWCAction {

    init {

        Log.e("AAA", "params: $params")
    }

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

