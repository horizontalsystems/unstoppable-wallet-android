package io.horizontalsystems.bankwallet.modules.createwallet

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewType
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinViewItem

class CreateWalletPresenter(
        private val presentationMode: PresentationMode,
        private val predefinedAccountType: PredefinedAccountType?,
        val view: CreateWalletModule.IView,
        val router: CreateWalletModule.IRouter,
        private val interactor: CreateWalletModule.IInteractor
) : ViewModel(), CreateWalletModule.IViewDelegate {

    private var accounts = mutableMapOf<PredefinedAccountType, Account>()
    private var wallets = mutableMapOf<Coin, Wallet>()

    override fun onLoad() {
        syncViewItems()
        syncCreateButton()
    }

    private fun syncCreateButton() {
        view.setCreateButton(wallets.isNotEmpty())
    }

    override fun onEnable(coin: Coin) {
        try {
            val account = resolveAccount(coin.type.predefinedAccountType)
            createWallet(coin, account)
        } catch (e: Exception) {
            syncViewItems()
        }
    }

    override fun onDisable(coin: Coin) {
        wallets.remove(coin)
        syncCreateButton()
    }

    override fun onSelect(coin: Coin) {
        view.showNotSupported(coin.type.predefinedAccountType)
    }

    override fun onCreateButtonClick() {
        if (wallets.isNotEmpty()) {
            val accounts = wallets.values.map { it.account }
            interactor.createAccounts(accounts)
            interactor.saveWallets(wallets.values.toList())
            if (presentationMode == PresentationMode.Initial) {
                router.startMainModule()
            } else {
                router.close()
            }
        }
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

    private fun filteredCoins(coins: List<Coin>) : List<Coin> {
        if(predefinedAccountType == null) {
            return coins
        }
        return coins.filter { it.type.predefinedAccountType == predefinedAccountType }
    }

    private fun resolveAccount(predefinedAccountType: PredefinedAccountType): Account {
        accounts[predefinedAccountType]?.let {
            return it
        }

        val account = interactor.account(predefinedAccountType)
        accounts[predefinedAccountType] = account
        return account
    }

    private fun createWallet(coin: Coin, account: Account) {
        val coinSettings = interactor.coinSettings(coin.type)

        wallets[coin] = Wallet(coin, account, coinSettings)

        syncCreateButton()
    }

}
