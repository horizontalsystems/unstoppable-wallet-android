package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.DefaultAccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet

class ManageWalletsPresenter(private val interactor: ManageWalletsModule.IInteractor, private val router: ManageWalletsModule.IRouter)
    : ManageWalletsModule.IViewDelegate, ManageWalletsModule.IInteractorDelegate {

    var view: ManageWalletsModule.IView? = null

    private var items = mutableListOf<ManageWalletItem>()
    private var popularItems = mutableListOf<ManageWalletItem>()
    private val popularCoinIds = listOf("BTC", "BCH", "ETH", "DASH", "EOS", "BNB")
    private var currentItem: ManageWalletItem? = null

    //  ViewDelegate

    override fun viewDidLoad() {
        val wallets = interactor.wallets

        val popularCoins = interactor.coins.filter { popularCoinIds.contains(it.coinId) }
        val coins = interactor.coins.filter { !popularCoinIds.contains(it.coinId) }

        popularItems = popularCoins.map { coin ->
            ManageWalletItem(coin, wallets.find { it.coin == coin })
        }.toMutableList()

        items = coins.map { coin ->
            ManageWalletItem(coin, wallets.find { it.coin == coin })
        }.toMutableList()
    }

    override fun onClickCreateKey() {
        val item = currentItem ?: return

        try {
            item.wallet = interactor.createWallet(item.coin)
            view?.showSuccess()
        } catch (e: Exception) {
            view?.showError(e)
        }
    }

    override fun onClickRestoreKey() {
        val item = currentItem ?: return
        val predefinedAccountType = interactor.predefinedAccountTypes.firstOrNull {
            it.defaultAccountType == item.coin.type.defaultAccountType
        } ?: return

        when (val accountType = item.coin.type.defaultAccountType) {
            is DefaultAccountType.Mnemonic -> {
                router.openRestoreWordsModule(accountType.wordsCount, predefinedAccountType.title)
            }
            is DefaultAccountType.Eos -> {
                router.openRestoreEosModule(predefinedAccountType.title)
            }
        }
    }

    override fun onClickCancel() {
        view?.updateCoins()
    }

    override fun onRestore(accountType: AccountType, syncMode: SyncMode?) {
        val item = currentItem ?: return

        try {
            item.wallet = interactor.restoreWallet(item.coin, accountType, syncMode)
        } catch (e: Exception) {
            view?.showError(e)
        }
    }

    override val popularItemsCount: Int
        get() = popularItems.size

    override fun popularItem(position: Int): ManageWalletViewItem {
        return viewItem(popularItems[position])
    }

    override val itemsCount: Int
        get() = items.size

    override fun item(position: Int): ManageWalletViewItem {
        return viewItem(items[position])
    }

    override fun enablePopularCoin(position: Int) {
        enable(popularItems[position])
    }

    override fun disablePopularCoin(position: Int) {
        disable(popularItems[position])
    }

    override fun enableCoin(position: Int) {
        enable(items[position])
    }

    override fun disableCoin(position: Int) {
        disable(items[position])
    }

    override fun saveChanges() {
        interactor.saveWallets((items + popularItems).mapNotNull { it.wallet })
    }

    //  InteractorDelegate

    override fun didSaveChanges() {
        router.close()
    }

    //  Private

    private fun enable(item: ManageWalletItem) {
        val coin = item.coin
        val wallet = interactor.wallet(coin)
        if (wallet == null) {
            currentItem = item
            val accountType = getPredefinedAccountType(coin.type.defaultAccountType)
            accountType?.let {
                view?.showNoAccountDialog(coin, it)
            }
        } else {
            item.wallet = wallet
        }
    }

    private fun getPredefinedAccountType(coinDefaultAccountType: DefaultAccountType): IPredefinedAccountType? {
        return interactor.predefinedAccountTypes.firstOrNull {
            it.defaultAccountType == coinDefaultAccountType
        }
    }

    private fun disable(item: ManageWalletItem) {
        item.wallet = null
    }

    private fun viewItem(item: ManageWalletItem): ManageWalletViewItem {
        return ManageWalletViewItem(item.coin, enabled = item.wallet != null)
    }
}

data class ManageWalletItem(val coin: Coin, var wallet: Wallet?)
data class ManageWalletViewItem(val coin: Coin, val enabled: Boolean)
