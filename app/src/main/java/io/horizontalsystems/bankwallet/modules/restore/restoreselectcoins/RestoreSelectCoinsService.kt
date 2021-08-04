package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsService
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*

class RestoreSelectCoinsService(
        private val accountType: AccountType,
        private val accountFactory: IAccountFactory,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val coinManager: ICoinManager,
        private val enableCoinsService: EnableCoinsService,
        private val restoreSettingsService: RestoreSettingsService,
        private val coinSettingsService: CoinSettingsService
) : Clearable {

    private var filter: String? = null
    private val disposables = CompositeDisposable()

    private val featuredCoins: List<Coin>
    private val coins: MutableList<Coin>
    val enabledCoins = mutableListOf<ConfiguredCoin>()

    private var restoreSettingsMap = mutableMapOf<Coin, RestoreSettings>()
    private var enableCoinServiceIsBusy = false

    val cancelEnableCoinAsync = PublishSubject.create<Coin>()

    val canRestore = BehaviorSubject.createDefault(false)
    val stateObservable = BehaviorSubject.create<State>()
    var state: State = State()
        set(value) {
            field = value
            stateObservable.onNext(value)
        }

    init {
        enableCoinsService.stateAsync
            .subscribe {
                enableCoinServiceIsBusy = it == EnableCoinsService.State.Loading
                syncCanRestore()
            }.let {
                disposables.add(it)
            }

        enableCoinsService.enableCoinsAsync
                .subscribeIO { coins ->
                    handleEnable(coins)
                }.let {
                    disposables.add(it)
                }

        restoreSettingsService.approveSettingsObservable
                .subscribeIO { coinWithSettings ->
                    handleApproveRestoreSettings(coinWithSettings.coin, coinWithSettings.settings)
                }.let {
                    disposables.add(it)
                }

        restoreSettingsService.rejectApproveSettingsObservable
                .subscribeIO { coin ->
                    handleRejectApproveRestoreSettings(coin)
                }.let {
                    disposables.add(it)
                }

        coinSettingsService.approveSettingsObservable
                .subscribeIO { coinWithSettings ->
                    handleApproveCoinSettings(coinWithSettings.coin, coinWithSettings.settingsList)
                }.let {
                    disposables.add(it)
                }

        coinSettingsService.rejectApproveSettingsObservable
                .subscribeIO { coin ->
                    handleRejectApproveCoinSettings(coin)
                }.let {
                    disposables.add(it)
                }

        featuredCoins = coinManager.groupedCoins.first
        coins = coinManager.groupedCoins.second.toMutableList()

        sortCoins()
        syncState()
    }

    private fun isEnabled(coin: Coin): Boolean {
        return enabledCoins.any { it.coin == coin }
    }

    private fun item(coin: Coin): Item {
        val enabled = isEnabled(coin)
        return Item(coin, enabled && coin.type.coinSettingTypes.isNotEmpty(), enabled)
    }

    private fun filtered(coins: List<Coin>): List<Coin> {
        return filter?.let {
            coins.filter { coin ->
                coin.title.contains(it, true) || coin.code.contains(it, true)
            }
        } ?: coins
    }

    private fun sortCoins() {
        coins.sortWith(compareByDescending<Coin> {
            isEnabled(it)
        }.thenBy {
            it.title.toLowerCase(Locale.ENGLISH)
        })
    }

    private fun syncState() {
        val filteredFeaturedCoins = filtered(featuredCoins)
        val filteredCoins = filtered(coins)

        state = State(
                filteredFeaturedCoins.map { item(it) },
                filteredCoins.map { item(it) }
        )
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty() && !enableCoinServiceIsBusy)
    }

    private fun configuredCoins(coin: Coin, settingsList: List<CoinSettings>) = when {
        settingsList.isEmpty() -> listOf(ConfiguredCoin(coin))
        else -> settingsList.map { ConfiguredCoin(coin, it) }
    }

    private fun handleApproveRestoreSettings(coin: Coin, settings: RestoreSettings = RestoreSettings()) {
        if (settings.isNotEmpty()) {
            restoreSettingsMap[coin] = settings
        }

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
            enable(configuredCoins)
            enableCoinsService.handle(coin.type, accountType)
        }
    }

    private fun handleRejectApproveCoinSettings(coin: Coin) {
        if (!isEnabled(coin)) {
            cancelEnableCoinAsync.onNext(coin)
        }
    }

    private fun applySettings(coin: Coin, configuredCoins: List<ConfiguredCoin>) {
        val existingConfiguredCoins = enabledCoins.filter { it.coin == coin }

        val newConfiguredCoins = configuredCoins.minus(existingConfiguredCoins)
        val removedConfiguredCoins = existingConfiguredCoins.minus(configuredCoins)

        enabledCoins.addAll(newConfiguredCoins)
        enabledCoins.removeAll(removedConfiguredCoins)
    }

    private fun handleEnable(coins: List<Coin>) {
        val allCoins = coinManager.coins

        val existingCoins = mutableListOf<Coin>()
        val newCoins = mutableListOf<Coin>()

        coins.forEach { coin ->
            if (notEnabled(coin)) {
                val existingCoin = allCoins.firstOrNull { it.type == coin.type }
                if (existingCoin != null) {
                    existingCoins.add(existingCoin)
                } else {
                    newCoins.add(coin)
                }
            }
        }

        if (newCoins.isNotEmpty()) {
            this.coins.addAll(newCoins)
            coinManager.save(newCoins)
        }

        val configuredCoins = coins.map { ConfiguredCoin(it) }
        enable(configuredCoins, true)
    }

    private fun notEnabled(coin: Coin) =
            enabledCoins.firstOrNull { it.coin.type == coin.type } == null

    private fun enable(configuredCoins: List<ConfiguredCoin>, sortCoins: Boolean = false) {
        enabledCoins.addAll(configuredCoins)

        if (sortCoins) {
            sortCoins()
        }

        syncState()
        syncCanRestore()
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
            restoreSettingsService.approveSettings(coin)
        }
    }

    fun disable(coin: Coin) {
        enabledCoins.removeIf { it.coin == coin }

        syncState()
        syncCanRestore()
    }

    fun configure(coin: Coin) {
        if (coin.type.coinSettingTypes.isEmpty()) return

        val configuredCoins = enabledCoins.filter { it.coin == coin }
        val settingsArray = configuredCoins.map { it.settings }

        coinSettingsService.approveSettings(coin, settingsArray)
    }

    fun restore() {
        val account = accountFactory.account(accountType, AccountOrigin.Restored, true)
        accountManager.save(account)

        restoreSettingsMap.forEach { coin, settings ->
            restoreSettingsService.save(settings, account, coin)
        }

        if (enabledCoins.isEmpty()) return

        val wallets = enabledCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    override fun clear() = disposables.clear()

    data class State(val featured: List<Item> = listOf(), val items: List<Item> = listOf())
    data class Item(val coin: Coin, val hasSettings: Boolean, val enabled: Boolean)

}
