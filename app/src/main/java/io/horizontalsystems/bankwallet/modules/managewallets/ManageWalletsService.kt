package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.eligibleTokens
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.restoreSettingTypes
import io.horizontalsystems.bankwallet.core.sortedByFilter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageWalletsService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: IWalletManager,
    accountManager: IAccountManager,
    private val restoreSettingsService: RestoreSettingsService,
) : Clearable {

    val itemsObservable = PublishSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    val accountType: AccountType?
        get() = account?.type

    private val account: Account? = accountManager.activeAccount
    private var wallets = setOf<Wallet>()
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
        return wallets.any { it.token == token }
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = walletList.toSet()
    }

    private fun fetchFullCoins(): List<FullCoin> {
        return if (filter.isBlank()) {
            val account = this.account ?: return emptyList()
            val featuredFullCoins = marketKit.fullCoins("", 100).toMutableList()
                .filter { it.eligibleTokens(account.type).isNotEmpty() }

            val featuredCoins = featuredFullCoins.map { it.coin }
            val enabledFullCoins = marketKit.fullCoins(
                coinUids = wallets.filter { !featuredCoins.contains(it.coin) }.map { it.coin.uid }
            )
            val customFullCoins = wallets.filter { it.token.isCustom }.map { it.token.fullCoin }

            featuredFullCoins + enabledFullCoins + customFullCoins
        } else if (isContractAddress(filter)) {
            val tokens = marketKit.tokens(filter)
            val coinUids = tokens.map { it.coin.uid }
            marketKit.fullCoins(coinUids)
        } else {
            marketKit.fullCoins(filter, 20)
        }
    }

    private fun isContractAddress(filter: String) = try {
        AddressValidator.validate(filter)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    private fun syncFullCoins() {
        fullCoins = fetchFullCoins()
    }

    private fun sortItems() {
        fullCoins = fullCoins.sortedByFilter(filter)
        sortedItems = fullCoins
            .map { getItemsForFullCoin(it) }
            .flatten()
            .sortedByDescending { it.enabled }
    }

    private fun getItemsForFullCoin(fullCoin: FullCoin): List<Item> {
        val accountType = account?.type ?: return listOf()

        val items = mutableListOf<Item>()
        fullCoin.eligibleTokens(accountType).forEach { token ->
            items.add(getItemForToken(token))
        }

        return items
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
        wallets.firstOrNull { it.token == token }?.let {
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
