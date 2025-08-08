package io.horizontalsystems.bankwallet.modules.walletconnect.stellar

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.MethodData
import io.horizontalsystems.bankwallet.modules.walletconnect.handler.UnsupportedMethodException

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
            WCActionStellarSignAndSubmitXdr(
                request.params,
                peerMetaData?.name ?: ""
            )
        }

        "stellar_signXDR" -> {
            WCActionStellarSignXdr(
                request.params,
                peerMetaData?.name ?: ""
            )
        }

        else -> throw UnsupportedMethodException(request.method)
    }

    override fun getAccountAddresses(account: Account): List<String> {
        return supportedChains.map {
            "$it:GADCIJ2UKQRWG6WHHPFKKLX7BYAWL7HDL54RUZO7M7UIHNQZL63C2I4Z"
        }
    }

    override fun getMethodData(method: String, chainInternalId: String?): MethodData {
        val title = when (method) {
            "stellar_signAndSubmitXDR" -> "Approve Transaction"
            "stellar_signXDR" -> "Sign Request"
            else -> method
        }

        return MethodData(title, "Stellar")
    }
}
