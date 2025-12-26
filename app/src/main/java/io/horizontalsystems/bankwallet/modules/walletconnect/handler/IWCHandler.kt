package io.horizontalsystems.bankwallet.modules.walletconnect.handler

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.request.AbstractWCAction

interface IWCHandler {
    val chainNamespace: String

    val supportedChains: List<String>
    val supportedMethods: List<String>
    val supportedEvents: List<String>

    fun getAction(
        request: Wallet.Model.SessionRequest.JSONRPCRequest,
        peerMetaData: Core.Model.AppMetaData?,
        chainInternalId: String?
    ): AbstractWCAction

    fun getAccountAddresses(account: Account): List<String>

    fun getMethodData(method: String, chainInternalId: String?): MethodData
    fun getChainName(chainInternalId: String): String?
}
