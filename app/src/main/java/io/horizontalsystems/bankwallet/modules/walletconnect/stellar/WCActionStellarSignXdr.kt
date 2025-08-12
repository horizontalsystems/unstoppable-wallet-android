package io.horizontalsystems.bankwallet.modules.walletconnect.stellar

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCActionState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.CoroutineScope

class WCActionStellarSignXdr(
    private val params: String,
    private val peerName: String,
) : AbstractWCAction() {

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        TODO("Not yet implemented")
    }

    override suspend fun performAction(): String {
        TODO("Not yet implemented")
    }

    override fun start(coroutineScope: CoroutineScope) {
        TODO("Not yet implemented")
    }

    override fun createState(): WCActionState {
        TODO("Not yet implemented")
    }
}
