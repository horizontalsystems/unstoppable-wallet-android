package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class ManageWalletsPresenter(
        private val interactor: ManageWalletsModule.IInteractor,
        private val router: ManageWalletsModule.IRouter)
    : ManageWalletsModule.IViewDelegate, ManageWalletsModule.IInteractorDelegate {

    var view: ManageWalletsModule.IView? = null

    private val wallets = mutableMapOf<Coin, Wallet>()

    //  ViewDelegate

    override fun viewDidLoad() {
        interactor.wallets.forEach { wallet ->
            wallets[wallet.coin] = wallet
        }

        syncViewItems()
    }

    override fun onEnable(coin: Coin) {
        val account = account(coin) ?: return

        val coinSettingsToRequest = interactor.coinSettingsToRequest(coin, account.origin)

        if (coinSettingsToRequest.isEmpty()) {
            createWallet(coin, account, mutableMapOf())
        } else {
            router.showCoinSettings(coin, coinSettingsToRequest)
        }
    }

    override fun onDisable(coin: Coin) {
        val wallet = wallets[coin] ?: return

        interactor.delete(wallet)
        wallets.remove(coin)
    }

    override fun onSelect(coin: Coin) {
        view?.showNoAccountDialog(coin, coin.type.predefinedAccountType)
    }

    override fun onSelectNewAccount(predefinedAccountType: PredefinedAccountType) {
        try {
            val account = interactor.createAccount(predefinedAccountType)
            handleCreated(account)
        } catch (e: Exception) {
            syncViewItems()
            view?.showError(e)
        }
    }

    override fun onSelectRestoreAccount(predefinedAccountType: PredefinedAccountType) {
        router.openRestore(predefinedAccountType)
    }

    override fun didRestore(accountType: AccountType) {
        val account = interactor.createRestoredAccount(accountType)
        handleCreated(account)
    }

    override fun onClickCancel() {
        syncViewItems()
    }

    override fun onSelect(coinSettings: MutableMap<CoinSetting, String>, coin: Coin) {
        val account = account(coin) ?: return
        createWallet(coin, account, coinSettings)
    }

    private fun account(coin: Coin): Account? {
        return interactor.accounts.firstOrNull { coin.type.canSupport(it.type) }
    }

    private fun viewItem(coin: Coin): CoinManageViewItem {
        val type: CoinManageViewType = when {
            coin.type.predefinedAccountType.isCreationSupported() -> {
                val enabled = wallets[coin] != null
                CoinManageViewType.CoinWithSwitch(enabled)
            }
            else -> {
                CoinManageViewType.CoinWithArrow
            }
        }

        return CoinManageViewItem(type, CoinViewItem(coin))
    }

    private fun syncViewItems() {
        val featuredCoinIds = interactor.featuredCoins.map { it.coinId }
        val featured = interactor.featuredCoins.map { viewItem(it) }
        val others = interactor.coins.filter { !featuredCoinIds.contains(it.coinId) }.map { viewItem(it) }
        val viewItems = mutableListOf<CoinManageViewItem>()

        if (featured.isNotEmpty()) {
            viewItems.addAll(featured)
            viewItems.add(CoinManageViewItem(CoinManageViewType.Divider))
        }
        viewItems.addAll(others)

        view?.setItems(viewItems)
    }

    private fun createWallet(coin: Coin, account: Account, requestedCoinSettings: CoinSettings) {
        val coinSettings = interactor.coinSettingsToSave(coin, account.origin, requestedCoinSettings)

        val wallet = Wallet(coin, account, coinSettings)

        interactor.save(wallet)
        wallets[coin] = wallet
    }

    private fun handleCreated(account: Account) {
        interactor.save(account)

        syncViewItems()
        view?.showSuccess()
    }
}
