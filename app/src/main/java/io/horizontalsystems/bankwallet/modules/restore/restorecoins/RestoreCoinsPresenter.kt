package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class RestoreCoinsPresenter(
        private val presentationMode: PresentationMode,
        private val predefinedAccountType: PredefinedAccountType,
        private val accountType: AccountType,
        val view: RestoreCoinsModule.IView,
        val router: RestoreCoinsModule.IRouter,
        private val interactor: RestoreCoinsModule.IInteractor
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
            val account = interactor.account(accountType)
            interactor.create(account)

            val wallets = enabledCoins.map {
                val coinSettings = interactor.getCoinSettings(it.type)
                Wallet(it, account, coinSettings)
            }

            interactor.saveWallets(wallets)

            when (presentationMode) {
                PresentationMode.Initial -> router.startMainModule()
                PresentationMode.InApp -> router.close()
            }
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
