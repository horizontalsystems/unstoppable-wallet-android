package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItem
import io.horizontalsystems.bankwallet.modules.managecoins.CoinToggleViewItemState

class RestoreCoinsPresenter(
        private val presentationMode: PresentationMode,
        private val predefinedAccountType: PredefinedAccountType,
        val view: RestoreCoinsModule.IView,
        val router: RestoreCoinsModule.IRouter,
        private val interactor: RestoreCoinsModule.IInteractor
) : ViewModel(), RestoreCoinsModule.IViewDelegate {

    private var enabledCoins = mutableMapOf<Coin, CoinSettings>()

    override fun onLoad() {
        view.setTitle(predefinedAccountType)

        syncViewItems()
        syncProceedButton()
    }

    override fun onEnable(viewItem: CoinToggleViewItem) {
        val coin = viewItem.coin
        val coinSettingsToRequest = interactor.coinSettingsToRequest(coin, AccountOrigin.Restored)
        if (coinSettingsToRequest.isEmpty()) {
            enable(coin, mutableMapOf())
        } else {
            router.showCoinSettings(coin, coinSettingsToRequest)
        }
    }

    override fun onDisable(viewItem: CoinToggleViewItem) {
        enabledCoins.remove(viewItem.coin)
        syncProceedButton()
    }

    override fun onProceedButtonClick() {
        if (enabledCoins.isNotEmpty()) {
            router.showRestore(predefinedAccountType)
        }
    }

    override fun onSelectCoinSettings(coinSettings: CoinSettings, coin: Coin) {
        enable(coin, coinSettings)
    }

    override fun onCancelSelectingCoinSettings() {
        syncViewItems()
    }

    override fun didRestore(accountType: AccountType) {
        val account = interactor.account(accountType)
        interactor.create(account)

        val wallets = enabledCoins.map { Wallet(it.key, account, it.value) }

        interactor.saveWallets(wallets)

        when (presentationMode) {
            PresentationMode.Initial -> router.startMainModule()
            PresentationMode.InApp -> router.close()
        }
    }


    private fun viewItem(coin: Coin): CoinToggleViewItem {
        val enabled = enabledCoins[coin] != null
        val state: CoinToggleViewItemState = CoinToggleViewItemState.ToggleVisible(enabled)
        return CoinToggleViewItem(coin, state)
    }

    private fun syncViewItems() {
        val featuredCoinIds = interactor.featuredCoins.map { it.coinId }
        val featured = filteredCoins(interactor.featuredCoins).map { viewItem(it) }
        val coins = filteredCoins(interactor.coins.filter { !featuredCoinIds.contains(it.coinId) }).map { viewItem(it) }

        view.setItems(featured, coins)
    }

    private fun filteredCoins(coins: List<Coin>): List<Coin> {
        return coins.filter { it.type.predefinedAccountType == predefinedAccountType }
    }

    private fun syncProceedButton() {
        view.setProceedButton(enabledCoins.isNotEmpty())
    }

    private fun enable(coin: Coin, requestedCoinSettings: CoinSettings) {
        enabledCoins[coin] = interactor.coinSettingsToSave(coin, AccountOrigin.Restored, requestedCoinSettings)
        syncProceedButton()
    }

}
