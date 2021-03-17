package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsService
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.Comparator

class RestoreSelectCoinsService(
        private val predefinedAccountType: PredefinedAccountType,
        private val accountType: AccountType,
        private val coinManager: ICoinManager,
        private val enableCoinsService: EnableCoinsService,
        private val blockchainSettingsService: BlockchainSettingsService)
    : RestoreSelectCoinsModule.IService, Clearable {

    private val disposables = CompositeDisposable()

    private var featuredCoins = listOf<Coin>()
    private var coins = listOf<Coin>()

    val cancelEnableCoinAsync = PublishSubject.create<Coin>()

    override val canRestore = BehaviorSubject.create<Boolean>()
    override val stateObservable = BehaviorSubject.create<State>()
    override var state: State = State()
        set(value) {
            field = value
            stateObservable.onNext(value)
        }

    override var enabledCoins: MutableList<Coin> = mutableListOf()
        private set

    init {
        enableCoinsService.enableCoinsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { coins ->
                    enable(coins, true)
                }.let {
                    disposables.add(it)
                }

        blockchainSettingsService.approveEnableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    handleApproveEnable(coin)
                }.let {
                    disposables.add(it)
                }

        blockchainSettingsService.rejectEnableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    cancelEnableCoinAsync.onNext(coin)
                }.let {
                    disposables.add(it)
                }

        syncCoins()
        syncState()
    }

    private fun syncCoins() {
        val (featuredCoins, regularCoins) = coinManager.groupedCoins

        this.featuredCoins = filteredCoins(featuredCoins)

        coins = filteredCoins(regularCoins).sortedWith(Comparator{ lhsCoin, rhsCoin ->
            val lhsEnabled = enabledCoins.contains(lhsCoin)
            val rhsEnabled = enabledCoins.contains(rhsCoin)

            if (lhsEnabled != rhsEnabled) {
                return@Comparator if (lhsEnabled) -1 else 1
            }

            return@Comparator lhsCoin.title.toLowerCase(Locale.ENGLISH).compareTo(rhsCoin.title.toLowerCase(Locale.ENGLISH))
        })
    }

    private fun handleApproveEnable(coin: Coin) {
        enable(listOf(coin))
        enableCoinsService.handle(coin.type, accountType)
    }

    private fun enable(coins: List<Coin>, resyncCoins: Boolean = false) {
        coins.forEach {
            enabledCoins.add(it)
        }

        if (resyncCoins) {
            syncCoins()
        }

        syncState()
        syncCanRestore()
    }

    override fun enable(coin: Coin, derivationSetting: DerivationSetting?) {
        blockchainSettingsService.approveEnable(coin, AccountOrigin.Restored)
    }

    override fun disable(coin: Coin) {
        enabledCoins.remove(coin)

        syncState()
        syncCanRestore()
    }

    override fun clear() {
    }

    private fun filteredCoins(coins: List<Coin>): List<Coin> {
        return coins.filter { it.type.predefinedAccountType == predefinedAccountType }
    }

    private fun item(coin: Coin): Item? {
        return Item(coin, enabledCoins.contains(coin))
    }

    private fun syncState() {
        state = State(featuredCoins.mapNotNull { item(it) }, coins.mapNotNull { item(it) })
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty())
    }


    data class State(var featured: List<Item>, var items: List<Item>) {
        constructor() : this(listOf(), listOf())
    }

    data class Item(val coin: Coin, val enabled: Boolean)

}
