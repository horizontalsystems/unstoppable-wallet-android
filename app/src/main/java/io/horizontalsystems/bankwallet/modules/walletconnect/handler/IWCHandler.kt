package io.horizontalsystems.bankwallet.modules.walletconnect.handler

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.dapp.core.HSDAppRequest

interface IWCHandler {
    val chainNamespace: String

    val supportedChains: List<String>
    val supportedMethods: List<String>
    val supportedEvents: List<String>

    fun getAction(request: HSDAppRequest, chainInternalId: String?): AbstractWCAction

    fun getAccountAddresses(account: Account): List<String>

    fun getMethodData(method: String, chainInternalId: String?): MethodData
    fun getChainName(chainInternalId: String): String?
}
