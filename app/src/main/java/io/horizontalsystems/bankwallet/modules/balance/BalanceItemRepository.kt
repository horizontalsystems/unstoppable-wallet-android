package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.Observable

class BalanceItemRepository(
    private val activeWalletRepository: ItemRepository<Wallet>,
    private val accountSettingManager: AccountSettingManager
) : ItemRepository<BalanceModule.BalanceItem> {

    override val itemsObservable: Observable<List<BalanceModule.BalanceItem>> =
        activeWalletRepository.itemsObservable
            .map { wallets ->
                wallets.map { wallet ->
                    BalanceModule.BalanceItem(wallet, isMainNet(wallet))
                }
            }

    override fun refresh() {
        activeWalletRepository.refresh()
    }

    private fun isMainNet(wallet: Wallet) = when (wallet.coin.type) {
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
