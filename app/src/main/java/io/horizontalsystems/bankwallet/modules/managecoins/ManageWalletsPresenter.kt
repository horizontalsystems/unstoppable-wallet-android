package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class ManageWalletsPresenter(
        private val interactor: ManageWalletsModule.IInteractor,
        val router: ManageWalletsModule.IRouter,
        val view: ManageWalletsModule.IView
) : ViewModel(), ManageWalletsModule.IViewDelegate, ManageWalletsModule.InteractorDelegate {

    private val wallets = mutableMapOf<Coin, Wallet>()
    private var walletWithSettings: Wallet? = null

    //  ViewDelegate

    override fun onLoad() {
        interactor.wallets.forEach { wallet ->
            wallets[wallet.coin] = wallet
        }

        interactor.subscribeForNewTokenAddition()

        syncViewItems()
    }

    override fun onEnable(coin: Coin) {
        val account = account(coin) ?: return
        val derivationSetting = interactor.derivationSetting(coin.type)

        if (account.origin == AccountOrigin.Restored && derivationSetting != null) {
            walletWithSettings = Wallet(coin, account)
            view.showDerivationSelectorDialog(AccountType.Derivation.values().toList(), derivationSetting.derivation, coin)
        } else {
            enableWallet(coin, account)
        }
    }

    override fun onSelectDerivationSetting(coin: Coin, derivation: AccountType.Derivation) {
        interactor.saveDerivationSetting(DerivationSetting(coin.type, derivation))

        walletWithSettings?.let {
            enableWallet(it.coin, it.account)
            walletWithSettings = null
        }
    }

    override fun onCancelDerivationSelectorDialog(coin: Coin) {
        syncViewItems()
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
            interactor.save(account)
            syncViewItems()
            view.showSuccess()
        } catch (e: Exception) {
            syncViewItems()
            view.showError(e)
        }
    }

    override fun onSelectRestoreAccount(predefinedAccountType: PredefinedAccountType) {
        router.openRestore(predefinedAccountType)
    }

    override fun onClickCancel() {
        syncViewItems()
    }

    override fun onAccountRestored() {
        syncViewItems()
    }

    override fun onCleared() {
        interactor.clear()
        super.onCleared()
    }

    override fun onNewTokenAdded() {
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

    private fun enableWallet(coin: Coin, account: Account) {
        if (account.origin == AccountOrigin.Created) {
            interactor.initializeSettingsWithDefault(coin.type)
        } else {
            interactor.initializeSettings(coin.type)
        }

        val wallet = Wallet(coin, account)

        interactor.save(wallet)
        wallets[coin] = wallet
    }

}
