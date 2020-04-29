package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class RestoreCoinsPresenter(
        val view: RestoreCoinsModule.IView,
        val router: RestoreCoinsModule.IRouter,
        private val interactor: RestoreCoinsModule.IInteractor,
        private val predefinedAccountType: PredefinedAccountType
) : ViewModel(), RestoreCoinsModule.IViewDelegate {

    private var enabledCoins = mutableListOf<Coin>()

    override fun onLoad() {
        syncViewItems()
        syncProceedButton()
    }

    override fun onEnable(coin: Coin) {
        enabledCoins.add(coin)
        syncProceedButton()
    }

    override fun onDisable(coin: Coin) {
        enabledCoins.remove(coin)
        syncProceedButton()
    }

    override fun onProceedButtonClick() {
        if (enabledCoins.isNotEmpty()) {
            if (predefinedAccountType == PredefinedAccountType.Standard) {
                val coinTypes = enabledCoins.map { it.type }
                router.showBlockchainSettingsList(coinTypes)
            } else {
                router.closeWithSelectedCoins(enabledCoins)
            }
        }
    }

    override fun onReturnFromBlockchainSettingsList() {
        if (enabledCoins.isNotEmpty()) {
            router.closeWithSelectedCoins(enabledCoins)
        }
    }

    private fun viewItem(coin: Coin): CoinManageViewItem {
        val enabled = enabledCoins.contains(coin)
        val type = CoinManageViewType.CoinWithSwitch(enabled)
        return CoinManageViewItem(type, CoinViewItem(coin))
    }

    private fun syncViewItems() {
        val featuredCoinIds = interactor.featuredCoins.map { it.coinId }
        val featured = filteredCoins(interactor.featuredCoins).map { viewItem(it) }
        val others = filteredCoins(interactor.coins.filter { !featuredCoinIds.contains(it.coinId) }).map { viewItem(it) }

        val viewItems = mutableListOf<CoinManageViewItem>()

        if (featured.isNotEmpty()) {
            viewItems.addAll(featured)
            viewItems.add(CoinManageViewItem(CoinManageViewType.Divider))
        }
        viewItems.addAll(others)

        view.setItems(viewItems)
    }

    private fun filteredCoins(coins: List<Coin>): List<Coin> {
        return coins.filter { it.type.predefinedAccountType == predefinedAccountType }
    }

    private fun syncProceedButton() {
        view.setProceedButton(enabledCoins.isNotEmpty())
    }

}
