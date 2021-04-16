package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.BinanceSmartChainKitManager
import io.horizontalsystems.bankwallet.core.managers.EthereumKitManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.ethereumkit.core.EthereumKit

class WalletConnectManager(
        private val accountManager: IAccountManager,
        private val ethereumKitManager: EthereumKitManager,
        private val binanceSmartChainKitManager: BinanceSmartChainKitManager
) {

    val activeAccount: Account?
        get() = accountManager.activeAccount

    fun evmKit(chainId: Int, account: Account): EthereumKit? = when (chainId) {
        1 -> ethereumKitManager.evmKit(account)
        56 -> binanceSmartChainKitManager.evmKit(account)
        else -> null
    }

}
