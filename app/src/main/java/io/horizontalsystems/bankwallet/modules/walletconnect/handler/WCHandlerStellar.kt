package io.horizontalsystems.bankwallet.modules.walletconnect.handler

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectActionStellarSignAndSubmitXdr

class WCHandlerStellar : IWCHandler {
    override val chainNamespace = "stellar"

    override val supportedChains = listOf("stellar:pubnet")
    override val supportedMethods = listOf("stellar_signAndSubmitXDR", "stellar_signXDR")
    override val supportedEvents = listOf<String>()

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

    override fun getAccountAddresses(account: Account): List<String> {
        return supportedChains.map {
            "$it:GADCIJ2UKQRWG6WHHPFKKLX7BYAWL7HDL54RUZO7M7UIHNQZL63C2I4Z"
        }
    }
}
