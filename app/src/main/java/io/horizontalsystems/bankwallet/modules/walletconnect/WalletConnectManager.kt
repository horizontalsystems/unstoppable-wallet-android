package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.core.managers.BinanceSmartChainKitManager
import io.horizontalsystems.bankwallet.core.managers.EthereumKitManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.ethereumkit.core.EthereumKit

class WalletConnectManager(
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val ethereumKitManager: EthereumKitManager,
        private val binanceSmartChainKitManager: BinanceSmartChainKitManager
) {

    fun currentAccount(chainId: Int): Account? = when (chainId) {
        1 -> predefinedAccountTypeManager.account(PredefinedAccountType.Standard)
        56 -> predefinedAccountTypeManager.account(PredefinedAccountType.Binance)
        else -> null
    }

    fun evmKit(chainId: Int, account: Account): EthereumKit? = when (chainId) {
        1 -> ethereumKitManager.evmKit(account)
        56 -> binanceSmartChainKitManager.evmKit(account)
        else -> null
    }

}
