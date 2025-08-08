package io.horizontalsystems.bankwallet.modules.walletconnect.handler

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectActionStellarSignAndSubmitXdr

class WCHandlerStellar : IWCHandler {
    override val chainNamespace = "stellar"

    override fun getAction(
        request: Wallet.Model.SessionRequest.JSONRPCRequest,
        peerMetaData: Core.Model.AppMetaData?,
        chainInternalId: String?,
    ) = when (request.method) {
        "stellar_signAndSubmitXDR" -> {
            WalletConnectActionStellarSignAndSubmitXdr(
                request.params,
                peerMetaData?.name ?: ""
            )
        }

        "stellar_signXDR" -> {
            TODO()
        }

        else -> throw UnsupportedMethodException(request.method)
    }

}
