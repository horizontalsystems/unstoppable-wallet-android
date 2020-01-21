package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class ManageWalletsPresenter(
        private val interactor: ManageWalletsModule.IInteractor,
        val showCloseButton: Boolean,
        val router: ManageWalletsModule.IRouter,
        val view: ManageWalletsModule.IView
) : ViewModel(), ManageWalletsModule.IViewDelegate {

    private val wallets = mutableMapOf<Coin, Wallet>()

    //  ViewDelegate

    override fun onLoad() {
        interactor.wallets.forEach { wallet ->
            wallets[wallet.coin] = wallet
        }

        syncViewItems()
    }

    override fun onEnable(coin: Coin) {
        val account = account(coin) ?: return
        createWallet(coin, account)
    }

    override fun onDisable(coin: Coin) {
        val wallet = wallets[coin] ?: return

        interactor.delete(wallet)
        wallets.remove(coin)
    }

    override fun onSelect(coin: Coin) {
        view.showNoAccountDialog(coin, coin.type.predefinedAccountType)
    }

    override fun onSelectNewAccount(predefinedAccountType: PredefinedAccountType) {
        try {
            val account = interactor.createAccount(predefinedAccountType)
            handleCreated(account)
        } catch (e: Exception) {
            syncViewItems()
            view.showError(e)
        }
    }

    override fun onSelectRestoreAccount(predefinedAccountType: PredefinedAccountType) {
        router.openRestore(predefinedAccountType)
    }

    override fun didRestore(accountType: AccountType) {
        val account = interactor.createRestoredAccount(accountType)
        handleCreated(account)

        if (accountType is AccountType.Mnemonic && accountType.words.size == 12) {
            router.showCoinSettings()
        } else {
            view.showSuccess()
        }
    }

    override fun onCoinSettingsClose() {
        view.showSuccess()
    }

    override fun onClickCancel() {
        syncViewItems()
    }

    private fun account(coin: Coin): Account? {
        return interactor.accounts.firstOrNull { coin.type.canSupport(it.type) }
    }

    private fun viewItem(coin: Coin): CoinManageViewItem {
        val hasAccount = account(coin) != null
        val type: CoinManageViewType = when {
            hasAccount -> CoinManageViewType.CoinWithSwitch(wallets[coin] != null)
            else -> CoinManageViewType.CoinWithArrow
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

        view.setItems(viewItems)
    }

    private fun createWallet(coin: Coin, account: Account) {
        val coinSettings = interactor.getCoinSettings(coin.type)

        val wallet = Wallet(coin, account, coinSettings)

        interactor.save(wallet)
        wallets[coin] = wallet
    }

    private fun handleCreated(account: Account) {
        interactor.save(account)

        syncViewItems()
    }
}
