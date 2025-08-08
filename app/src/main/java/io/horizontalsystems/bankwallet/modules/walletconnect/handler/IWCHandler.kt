package io.horizontalsystems.bankwallet.modules.walletconnect.handler

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.modules.walletconnect.request.IWCAction

interface IWCHandler {
    val chainNamespace: String

    fun getAction(
        request: Wallet.Model.SessionRequest.JSONRPCRequest,
        peerMetaData: Core.Model.AppMetaData?,
        chainInternalId: String?
    ): IWCAction
}
