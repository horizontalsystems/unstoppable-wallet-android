package com.quantum.wallet.bankwallet.modules.walletconnect.handler

import com.reown.android.Core
import com.reown.walletkit.client.Wallet
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.modules.walletconnect.request.AbstractWCAction

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
