package io.horizontalsystems.bankwallet.modules.managecoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class ManageWalletsPresenter(
        private val interactor: ManageWalletsModule.IInteractor,
        private val isColdStart: Boolean,
        val showCloseButton: Boolean,
        val router: ManageWalletsModule.IRouter,
        val view: ManageWalletsModule.IView
) : ViewModel(), ManageWalletsModule.IViewDelegate {

    private val wallets = mutableMapOf<Coin, Wallet>()
    private var walletWithSettings: Wallet? = null

    //  ViewDelegate

    override fun onLoad() {
        if (isColdStart) {
            interactor.loadAccounts()
            interactor.loadWallets()
        }

        interactor.wallets.forEach { wallet ->
            wallets[wallet.coin] = wallet
        }

        syncViewItems()
    }

    override fun onEnable(coin: Coin) {
        val account = account(coin) ?: return
        if (account.origin == AccountOrigin.Restored && (interactor.derivation(coin.type) != null || interactor.syncMode(coin.type) != null)) {
            walletWithSettings = Wallet(coin, account)
            router.showSettings(coin.type)
            return
        }
        enableWallet(coin, account)
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

    override fun onBlockchainSettingsApproved() {
        walletWithSettings?.let {
            enableWallet(it.coin, it.account)
            walletWithSettings = null
        }
    }

    override fun onBlockchainSettingsCancel() {
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
        val forCreate = account.origin == AccountOrigin.Created
        val derivation: Derivation? = interactor.derivation(coin.type, forCreate)
        val syncMode: SyncMode? = interactor.syncMode(coin.type, forCreate)

        derivation?.let {
            interactor.saveDerivation(coin.type, derivation)
        }
        syncMode?.let {
            interactor.saveSyncMode(coin.type, syncMode)
        }

        val wallet = Wallet(coin, account)

        interactor.save(wallet)
        wallets[coin] = wallet
    }

}
