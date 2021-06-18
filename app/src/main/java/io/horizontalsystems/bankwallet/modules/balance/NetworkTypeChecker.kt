package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType

class NetworkTypeChecker(private val accountSettingManager: AccountSettingManager) {

    fun isMainNet(wallet: Wallet) = when (wallet.coin.type) {
        is CoinType.Ethereum,
        is CoinType.Erc20 -> {
            accountSettingManager.ethereumNetwork(wallet.account).networkType.isMainNet
        }
        is CoinType.BinanceSmartChain -> {
            accountSettingManager.binanceSmartChainNetwork(wallet.account).networkType.isMainNet
        }
        else -> true
    }

}
