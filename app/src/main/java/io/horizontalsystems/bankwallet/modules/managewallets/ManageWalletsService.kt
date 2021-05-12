package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.CoinSettingsService
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSettingsService
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

class ManageWalletsService(
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        accountManager: IAccountManager,
        private val restoreSettingsService: RestoreSettingsService,
        private val coinSettingsService: CoinSettingsService)
    : Clearable {

    val stateAsync = PublishSubject.create<State>()
    var state = State(listOf(), listOf())
        private set(value) {
            field = value
            stateAsync.onNext(value)
        }
    val cancelEnableCoinAsync = PublishSubject.create<Coin>()

    private val account: Account = accountManager.activeAccount!!
    private var wallets = setOf<Wallet>()
    private var featuredCoins = listOf<Coin>()
    private var coins = listOf<Coin>()
    private var addedCoins = mutableListOf<Coin>()

    private val disposables = CompositeDisposable()

    private var filter: String? = null

    init {
        walletManager.activeWalletsUpdatedObservable
                .subscribeIO {
                    handleUpdated(it)
                }
                .let {
                    disposables.add(it)
                }

        coinManager.coinAddedObservable
                .subscribeIO {
                    handleAdded(it)
                }
                .let {
                    disposables.add(it)
                }

        restoreSettingsService.approveSettingsObservable
                .subscribeIO { coinWithSettings ->
                    handleApproveRestoreSettings(coinWithSettings.coin, coinWithSettings.settings)
                }
                .let {
                    disposables.add(it)
                }

        restoreSettingsService.rejectApproveSettingsObservable
                .subscribeIO { coin ->
                    handleRejectApproveRestoreSettings(coin)
                }
                .let {
                    disposables.add(it)
                }

        coinSettingsService.approveSettingsObservable
                .subscribeIO { coinWithSettings ->
                    handleApproveCoinSettings(coinWithSettings.coin, coinWithSettings.settingsList)
                }
                .let {
                    disposables.add(it)
                }

        coinSettingsService.rejectApproveSettingsObservable
                .subscribeIO { coin ->
                    handleRejectApproveCoinSettings(coin)
                }
                .let {
                    disposables.add(it)
                }

        syncCoins()
        sync(walletManager.activeWallets)
        sortCoins()
        syncState()
    }

    private fun syncCoins() {
        featuredCoins = coinManager.groupedCoins.first
        coins = coinManager.groupedCoins.second
    }

    private fun isEnabled(coin: Coin): Boolean {
        return wallets.any { it.coin == coin }
    }

    private fun sortCoins() {
        coins = coins.sortedWith(compareByDescending<Coin> {
            addedCoins.contains(it)
        }.thenByDescending {
            isEnabled(it)
        }.thenBy {
            it.title.toLowerCase(Locale.ENGLISH)
        })
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = walletList.toSet()
    }

    private fun item(coin: Coin): Item {
        val enabled = isEnabled(coin)
        return Item(coin, enabled && coin.type.coinSettingTypes.isNotEmpty(), enabled)
    }

    private fun filtered(coins: List<Coin>): List<Coin> {
        return filter?.let { filter ->
            coins.filter { coin ->
                coin.title.contains(filter, true) || coin.code.contains(filter, true)
            }
        } ?: coins
    }

    private fun syncState() {
        val filteredFeaturedCoins = filtered(featuredCoins)
        val filteredCoins = filtered(coins)

        state = State(filteredFeaturedCoins.map { item(it) }, filteredCoins.map { item(it) })
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)
        syncState()
    }

    private fun configuredCoins(coin: Coin, settingsList: List<CoinSettings>) = when {
        settingsList.isEmpty() -> listOf(ConfiguredCoin(coin))
        else -> settingsList.map { ConfiguredCoin(coin, it) }
    }

    private fun handleApproveRestoreSettings(coin: Coin, settings: RestoreSettings = RestoreSettings()) {
        restoreSettingsService.save(settings, account, coin)

        if (coin.type.coinSettingTypes.isEmpty()) {
            handleApproveCoinSettings(coin)
        } else {
            coinSettingsService.approveSettings(coin, coin.type.defaultSettingsArray)
        }
    }

    private fun handleRejectApproveRestoreSettings(coin: Coin) {
        cancelEnableCoinAsync.onNext(coin)
    }

    private fun handleApproveCoinSettings(coin: Coin, settingsList: List<CoinSettings> = listOf()) {
        val configuredCoins = configuredCoins(coin, settingsList)

        if (isEnabled(coin)) {
            applySettings(coin, configuredCoins)
        } else {
            val wallets = configuredCoins.map { Wallet(it, account) }
            walletManager.save(wallets)
        }
    }

    private fun handleRejectApproveCoinSettings(coin: Coin) {
        if (!isEnabled(coin)) {
            cancelEnableCoinAsync.onNext(coin)
        }
    }

    private fun applySettings(coin: Coin, configuredCoins: List<ConfiguredCoin>) {
        val existingWallets = wallets.filter { it.coin == coin }
        val existingConfiguredCoins = existingWallets.map { it.configuredCoin }

        val newConfiguredCoins = configuredCoins.minus(existingConfiguredCoins)
        val removedWallets = existingWallets.filter { !configuredCoins.contains(it.configuredCoin) }

        val newWallets = newConfiguredCoins.map { Wallet(it, account) }

        if (newWallets.isNotEmpty() || removedWallets.isNotEmpty()) {
            walletManager.handle(newWallets, removedWallets)
        }
    }

    private fun handleAdded(coins: List<Coin>) {
        addedCoins.addAll(coins)

        syncCoins()
        sortCoins()
        syncState()
    }

    fun setFilter(v: String?) {
        filter = v

        sortCoins()
        syncState()
    }

    fun enable(coin: Coin) {
        if (coin.type.restoreSettingTypes.isEmpty()) {
            handleApproveRestoreSettings(coin)
        } else {
            restoreSettingsService.approveSettings(coin, account)
        }
    }

    fun disable(coin: Coin) {
        val walletsToDelete = wallets.filter { it.coin == coin }
        walletManager.delete(walletsToDelete)
    }

    fun configure(coin: Coin) {
        if (coin.type.coinSettingTypes.isEmpty()) return

        val coinWallets = wallets.filter { it.coin == coin }
        val settingsList = coinWallets.map { it.configuredCoin.settings }

        coinSettingsService.approveSettings(coin, settingsList)
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(val coin: Coin, val hasSettings: Boolean, val enabled: Boolean)
    data class State(val featuredItems: List<Item>, val items: List<Item>)
}
