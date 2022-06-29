package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.xxxkit.MarketKit
import io.horizontalsystems.xxxkit.models.Coin
import io.horizontalsystems.xxxkit.models.FullCoin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageWalletsService(
    private val marketKit: MarketKit,
    private val walletManager: IWalletManager,
    accountManager: IAccountManager,
    private val enableCoinService: EnableCoinService
) : Clearable {

    val itemsObservable = PublishSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    val cancelEnableCoinObservable = PublishSubject.create<Coin>()

    private val account: Account? = accountManager.activeAccount
    private var wallets = setOf<Wallet>()
    private var fullCoins = listOf<FullCoin>()

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

        enableCoinService.enableCoinObservable
            .subscribeIO { (configuredPlatformCoins, settings) ->
                handleEnableCoin(configuredPlatformCoins, settings)
            }.let {
                disposables.add(it)
            }

        enableCoinService.cancelEnableCoinObservable
            .subscribeIO { fullCoin ->
                handleCancelEnable(fullCoin)
            }.let { disposables.add(it) }


        sync(walletManager.activeWallets)
        syncFullCoins()
        sortFullCoins()
        syncState()
    }

    private fun isEnabled(coin: Coin): Boolean {
        return wallets.any { it.token.coin == coin }
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = walletList.toSet()
    }

    private fun fetchFullCoins(): List<FullCoin> {
        return if (filter.isBlank()) {
            val featuredFullCoins =
                marketKit.fullCoins("", 100).toMutableList().filter { it.supportedTokens.isNotEmpty() }

            val featuredCoins = featuredFullCoins.map { it.coin }
            val enabledFullCoins = marketKit.fullCoins(
                coinUids = wallets.filter { !featuredCoins.contains(it.coin) }.map { it.coin.uid }
            )
            val customFullCoins = wallets.filter { it.token.isCustom }.map { it.token.fullCoin }

            featuredFullCoins + enabledFullCoins + customFullCoins
        } else {
            marketKit.fullCoins(filter, 20).toMutableList()
        }
    }

    private fun syncFullCoins() {
        fullCoins = fetchFullCoins()
    }

    private fun sortFullCoins() {
        fullCoins = fullCoins.sortedByFilter(filter) {
            isEnabled(it.coin)
        }
    }

    private fun item(fullCoin: FullCoin): Item {
        val itemState = if (fullCoin.supportedTokens.isEmpty()) {
            ItemState.Unsupported
        } else {
            val enabled = isEnabled(fullCoin.coin)
            ItemState.Supported(
                enabled = enabled,
                hasSettings = enabled && hasSettingsOrPlatforms(fullCoin)
            )
        }

        return Item(fullCoin, itemState)
    }

    private fun hasSettingsOrPlatforms(fullCoin: FullCoin): Boolean {
        val supportedTokens = fullCoin.supportedTokens

        return if (supportedTokens.size == 1) {
            val token = supportedTokens[0]
            token.blockchainType.coinSettingTypes.isNotEmpty()
        } else {
            true
        }
    }

    private fun syncState() {
        items = fullCoins.map { item(it) }
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)

        val newFullCons = fetchFullCoins()
        if (newFullCons.size > fullCoins.size) {
            fullCoins = newFullCons
            sortFullCoins()
        }

        syncState()
    }

    private fun handleEnableCoin(
        configuredTokens: List<ConfiguredToken>, restoreSettings: RestoreSettings
    ) {
        val account = this.account ?: return
        val coin = configuredTokens.firstOrNull()?.token?.coin ?: return

        if (restoreSettings.isNotEmpty() && configuredTokens.size == 1) {
            enableCoinService.save(restoreSettings, account, configuredTokens.first().token.blockchainType)
        }

        val existingWallets = wallets.filter { it.coin == coin }
        val existingConfiguredPlatformCoins = existingWallets.map { it.configuredToken }
        val newConfiguredPlatformCoins = configuredTokens.minus(existingConfiguredPlatformCoins)

        val removedWallets = existingWallets.filter { !configuredTokens.contains(it.configuredToken) }
        val newWallets = newConfiguredPlatformCoins.map { Wallet(it, account) }

        if (newWallets.isNotEmpty() || removedWallets.isNotEmpty()) {
            walletManager.handle(newWallets, removedWallets)
        }
    }

    private fun handleCancelEnable(fullCoin: FullCoin) {
        if (!isEnabled(fullCoin.coin)) {
            cancelEnableCoinObservable.onNext(fullCoin.coin)
        }
    }

    fun setFilter(filter: String) {
        this.filter = filter

        syncFullCoins()
        sortFullCoins()
        syncState()
    }

    fun enable(uid: String) {
        val fullCoin = fullCoins.firstOrNull { it.coin.uid == uid } ?: return
        enable(fullCoin)
    }

    fun enable(fullCoin: FullCoin) {
        enableCoinService.enable(fullCoin, account)
    }

    fun disable(uid: String) {
        val walletsToDelete = wallets.filter { it.coin.uid == uid }
        walletManager.delete(walletsToDelete)
    }

    fun configure(uid: String) {
        val fullCoin = fullCoins.firstOrNull { it.coin.uid == uid } ?: return
        val coinWallets = wallets.filter { it.coin == fullCoin.coin }
        enableCoinService.configure(fullCoin, coinWallets.map { it.configuredToken })
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(
        val fullCoin: FullCoin,
        val state: ItemState
    )

    sealed class ItemState {
        object Unsupported : ItemState()
        class Supported(val enabled: Boolean, val hasSettings: Boolean) : ItemState()
    }
}
