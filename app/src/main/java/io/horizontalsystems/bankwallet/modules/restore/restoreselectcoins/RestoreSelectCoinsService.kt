package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsService
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.MarketCoin
import io.horizontalsystems.marketkit.models.PlatformCoin
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
    private val enableCoinService: EnableCoinService
) : Clearable {

    private var filter: String = ""
    private val disposables = CompositeDisposable()

    private var marketCoins: MutableList<MarketCoin> = mutableListOf()
    val enabledCoins = mutableListOf<ConfiguredPlatformCoin>()

    private var restoreSettingsMap = mutableMapOf<PlatformCoin, RestoreSettings>()
    private var enableCoinServiceIsBusy = false

    val cancelEnableCoinObservable = PublishSubject.create<Coin>()
    val canRestore = BehaviorSubject.createDefault(false)

    val itemsObservable = BehaviorSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    init {
        enableCoinsService.stateAsync
            .subscribe {
                enableCoinServiceIsBusy = it == EnableCoinsService.State.Loading
                syncCanRestore()
            }.let {
                disposables.add(it)
            }

        enableCoinsService.enableCoinTypesAsync
            .subscribeIO { coins ->
                handleEnable(coins)
            }.let {
                disposables.add(it)
            }

        enableCoinService.enableCoinObservable
            .subscribeIO { (configuredPlatformCoins, settings) ->
                handleEnableCoin(configuredPlatformCoins, settings)
            }.let {
                disposables.add(it)
            }

        enableCoinService.cancelEnableCoinObservable
            .subscribeIO { coin ->
                handleCancelEnable(coin)
            }.let { disposables.add(it) }

        syncMarketCoins()
        sortMarketCoins()
        syncState()
    }

    private fun syncMarketCoins() {
        marketCoins = if (filter.isNotBlank()) {
            coinManager.featuredMarketCoins(enabledCoins.map { it.platformCoin.coinType }).toMutableList()
        } else {
            coinManager.marketCoins(filter, 20).toMutableList()
        }
    }

    private fun handleEnableCoin(
        configuredPlatformCoins: List<ConfiguredPlatformCoin>,
        restoreSettings: RestoreSettings
    ) {
        val platformCoin = configuredPlatformCoins.firstOrNull()?.platformCoin ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[platformCoin] = restoreSettings
        }

        val existingConfiguredPlatformCoins = enabledCoins.filter { it.platformCoin.coin == platformCoin.coin }
        val newConfiguredPlatformCoins = configuredPlatformCoins.minus(existingConfiguredPlatformCoins)
        val removedConfiguredPlatformCoins = existingConfiguredPlatformCoins.minus(configuredPlatformCoins)

        enabledCoins.addAll(newConfiguredPlatformCoins)
        enabledCoins.removeAll(removedConfiguredPlatformCoins)

        syncCanRestore()
        syncState()
        enableCoinsService.handle(newConfiguredPlatformCoins.map { it.platformCoin.coinType }, accountType)
    }

    private fun handleCancelEnable(coin: Coin) {
        if (!isEnabled(coin)) {
            cancelEnableCoinObservable.onNext(coin)
        }
    }

    private fun isEnabled(coin: Coin): Boolean {
        return enabledCoins.any { it.platformCoin.coin == coin }
    }

    private fun item(marketCoin: MarketCoin): Item {
        val supportedPlatforms = marketCoin.platforms.filter { it.coinType.isSupported }
        val marketCoin = MarketCoin(marketCoin.coin, supportedPlatforms)

        val itemState = if (marketCoin.platforms.isEmpty()) {
            ItemState.Unsupported
        } else {
            val enabled = isEnabled(marketCoin.coin)
            ItemState.Supported(
                enabled = enabled,
                hasSettings = enabled && hasSettingsOrPlatforms(marketCoin)
            )
        }

        return Item(marketCoin, itemState)
    }

    private fun hasSettingsOrPlatforms(marketCoin: MarketCoin) =
        if (marketCoin.platforms.size == 1) {
            val platform = marketCoin.platforms[0]
            platform.coinType.coinSettingTypes.isNotEmpty()
        } else {
            true
        }

    private fun sortMarketCoins() {
        marketCoins.sortWith(compareByDescending<MarketCoin> {
            isEnabled(it.coin)
        }.thenBy {
            it.coin.marketCapRank
        }.thenBy {
            it.coin.name.lowercase(Locale.ENGLISH)
        })
    }

    private fun syncState() {
        items = marketCoins.map { item(it) }
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty() && !enableCoinServiceIsBusy)
    }

    private fun handleEnable(coinTypes: List<CoinType>) {
        val platformCoins = coinManager.getPlatformCoinsByCoinTypeIds(coinTypes.map { it.id })
        val configuredPlatformCoins = platformCoins.map { ConfiguredPlatformCoin(it) }
        enabledCoins.addAll(configuredPlatformCoins)

        syncMarketCoins()
        sortMarketCoins()
        syncState()
    }

    fun setFilter(filter: String) {
        this.filter = filter

        syncMarketCoins()
        sortMarketCoins()
        syncState()
    }

    fun enable(marketCoin: MarketCoin) {
        enableCoinService.enable(marketCoin)
    }

    fun disable(marketCoin: MarketCoin) {
        enabledCoins.removeIf { it.platformCoin.coin == marketCoin.coin }

        syncState()
        syncCanRestore()
    }

    fun configure(marketCoin: MarketCoin) {
        enableCoinService.configure(marketCoin, enabledCoins.filter { it.platformCoin.coin == marketCoin.coin })
    }

    fun restore() {
        val account = accountFactory.account(accountType, AccountOrigin.Restored, true)
        accountManager.save(account)

        restoreSettingsMap.forEach { (platformCoin, settings) ->
            enableCoinService.save(settings, account, platformCoin.coinType)
        }

        if (enabledCoins.isEmpty()) return

        val wallets = enabledCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    override fun clear() = disposables.clear()

    data class Item(
        val marketCoin: MarketCoin,
        val state: ItemState
    )

    sealed class ItemState {
        object Unsupported : ItemState()
        class Supported(val enabled: Boolean, val hasSettings: Boolean) : ItemState()
    }
}
