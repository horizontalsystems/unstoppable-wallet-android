package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.MarketCoin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.*

class ManageWalletsService(
    private val coinManager: ICoinManager,
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

    private val account: Account = accountManager.activeAccount!!
    private var wallets = setOf<Wallet>()
    private var marketCoins = mutableListOf<MarketCoin>()

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
            .subscribeIO { coin ->
                handleCancelEnable(coin)
            }.let { disposables.add(it) }


        sync(walletManager.activeWallets)
        syncMarketCoins()
        sortMarketCoins()
        syncState()
    }

    private fun isEnabled(coin: Coin): Boolean {
        return wallets.any { it.coin == coin }
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = walletList.toSet()
    }

    private fun syncMarketCoins() {
        marketCoins = if (filter.isNotBlank()) {
            coinManager.featuredMarketCoins(wallets.map { it.coinType }).toMutableList()
        } else {
            coinManager.marketCoins(filter, 20).toMutableList()
        }
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

    private fun syncState() {
        items = marketCoins.map { item(it) }
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)
        syncState()
    }

    private fun handleEnableCoin(
        configuredPlatformCoins: List<ConfiguredPlatformCoin>, restoreSettings: RestoreSettings
    ) {
        val coin = configuredPlatformCoins.firstOrNull()?.platformCoin?.coin ?: return

        if (restoreSettings.isNotEmpty() && configuredPlatformCoins.size == 1) {
            enableCoinService.save(restoreSettings, account, configuredPlatformCoins.first().platformCoin.coinType)
        }

        val existingWallets = wallets.filter { it.coin == coin }
        val existingConfiguredPlatformCoins = existingWallets.map { it.configuredPlatformCoin }
        val newConfiguredPlatformCoins = configuredPlatformCoins.minus(existingConfiguredPlatformCoins)

        val removedWallets = existingWallets.filter { !configuredPlatformCoins.contains(it.configuredPlatformCoin) }
        val newWallets = newConfiguredPlatformCoins.map { Wallet(it, account) }

        if (newWallets.isNotEmpty() || removedWallets.isNotEmpty()) {
            walletManager.handle(newWallets, removedWallets)
        }
    }

    private fun handleCancelEnable(coin: Coin) {
        if (!isEnabled(coin)) {
            cancelEnableCoinObservable.onNext(coin)
        }
    }

    fun setFilter(v: String) {
        filter = v

        syncMarketCoins()
        sortMarketCoins()
        syncState()
    }

    fun enable(marketCoin: MarketCoin) {
        enableCoinService.enable(marketCoin, account)
    }

    fun disable(marketCoin: MarketCoin) {
        val walletsToDelete = wallets.filter { it.coin == marketCoin.coin }
        walletManager.delete(walletsToDelete)
    }

    fun configure(marketCoin: MarketCoin) {
        val coinWallets = wallets.filter { it.coin == marketCoin.coin }
        enableCoinService.configure(marketCoin, coinWallets.map { it.configuredPlatformCoin })
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(
        val marketCoin: MarketCoin,
        val state: ItemState
    )

    sealed class ItemState {
        object Unsupported : ItemState()
        class Supported(val enabled: Boolean, val hasSettings: Boolean) : ItemState()
    }
}
