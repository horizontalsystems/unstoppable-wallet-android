package cash.p.terminal.modules.managewallets

import cash.p.terminal.core.Clearable
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.isDefault
import cash.p.terminal.core.isNative
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.order
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import cash.p.terminal.modules.receivemain.FullCoinsProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageWalletsService(
    private val walletManager: IWalletManager,
    private val restoreSettingsService: RestoreSettingsService,
    private val fullCoinsProvider: FullCoinsProvider?,
    private val account: Account?
) : Clearable {

    val itemsObservable = PublishSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    val accountType: AccountType?
        get() = account?.type

    private var fullCoins = listOf<FullCoin>()
    private var sortedItems = listOf<Item>()

    private val disposables = CompositeDisposable()

    private var filter: String = ""

    init {
        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                handleUpdated(it)
            }
            .let {
                disposables.add(it)
            }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO {
                enable(it.token, it.settings)
            }.let {
                disposables.add(it)
            }

        sync(walletManager.activeWallets)
        syncFullCoins()
        sortItems()
        syncState()
    }

    private fun isEnabled(token: Token): Boolean {
        return walletManager.activeWallets.any { it.token == token }
    }

    private fun sync(walletList: List<Wallet>) {
        fullCoinsProvider?.setActiveWallets(walletList)
    }

    private fun fetchFullCoins(): List<FullCoin> {
        return fullCoinsProvider?.getItems() ?: listOf()
    }

    private fun syncFullCoins() {
        fullCoins = fetchFullCoins()
    }

    private fun sortItems() {
        sortedItems = fullCoins
            .map { getItemsForFullCoin(it) }
            .flatten()
            .sortedWith(
                compareByDescending<Item> {
                    it.enabled
                }.thenBy {
                    it.token.blockchain.type.order
                }
            )
    }

    private fun getItemsForFullCoin(fullCoin: FullCoin): List<Item> {
        val accountType = account?.type ?: return listOf()
        val eligibleTokens = fullCoin.eligibleTokens(accountType)

        val tokens = if (filter.isNotBlank()) {
            eligibleTokens
        } else if (
            eligibleTokens.all { it.type is TokenType.Derived } ||
            eligibleTokens.all { it.type is TokenType.AddressTyped }
        ) {
            eligibleTokens.filter { isEnabled(it) || it.type.isDefault }
        } else {
            eligibleTokens.filter { isEnabled(it) || it.type.isNative }
        }

        return tokens.map { getItemForToken(it) }
    }

    private fun getItemForToken(token: Token): Item {
        val enabled = isEnabled(token)

        return Item(
            token = token,
            enabled = enabled,
            hasInfo = hasInfo(token, enabled)
        )
    }

    private fun hasInfo(token: Token, enabled: Boolean) = when (token.type) {
        is TokenType.Native -> token.blockchainType is BlockchainType.Zcash && enabled
        is TokenType.Derived,
        is TokenType.AddressTyped,
        is TokenType.Eip20,
        is TokenType.Bep2,
        is TokenType.Spl -> true
        else -> false
    }

    private fun syncState() {
        items = sortedItems
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)

        val newFullCons = fetchFullCoins()
        if (newFullCons.size > fullCoins.size) {
            fullCoins = newFullCons
            sortItems()
        }

        syncState()
    }

    private fun updateSortedItems(token: Token, enable: Boolean) {
        sortedItems = sortedItems.map { item ->
            if (item.token == token) {
                item.copy(enabled = enable)
            } else {
                item
            }
        }
    }

    private fun enable(token: Token, restoreSettings: RestoreSettings) {
        val account = this.account ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsService.save(restoreSettings, account, token.blockchainType)
        }

        walletManager.save(listOf(Wallet(token, account)))

        updateSortedItems(token, true)
    }

    fun setFilter(filter: String) {
        this.filter = filter
        fullCoinsProvider?.setQuery(filter)

        syncFullCoins()
        sortItems()
        syncState()
    }

    fun enable(token: Token) {
        val account = this.account ?: return

        if (token.blockchainType.restoreSettingTypes.isNotEmpty()) {
            restoreSettingsService.approveSettings(token, account)
        } else {
            enable(token, RestoreSettings())
        }
    }

    fun disable(token: Token) {
        walletManager.activeWallets
            .firstOrNull { it.token == token }
            ?.let {
                walletManager.delete(listOf(it))
                updateSortedItems(token, false)
            }
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(
        val token: Token,
        val enabled: Boolean,
        val hasInfo: Boolean
    )
}
