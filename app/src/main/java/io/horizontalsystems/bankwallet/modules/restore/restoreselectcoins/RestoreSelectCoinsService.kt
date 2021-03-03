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

class RestoreSelectCoinsService(
        private val predefinedAccountType: PredefinedAccountType,
        private val accountType: AccountType,
        private val coinManager: ICoinManager,
        private val enableCoinsService: EnableCoinsService,
        private val blockchainSettingsService: BlockchainSettingsService)
    : RestoreSelectCoinsModule.IService, Clearable {

    private val disposables = CompositeDisposable()

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
                    enable(coins)
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

        syncState()
    }

    private fun handleApproveEnable(coin: Coin) {
        enable(listOf(coin))
        enableCoinsService.handle(coin.type, accountType)
    }

    private fun enable(coins: List<Coin>) {
        coins.forEach {
            enabledCoins.add(it)
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
        val featuredCoins = filteredCoins(coinManager.featuredCoins)
        val coins = filteredCoins(coinManager.coins).filter { !featuredCoins.contains(it) }

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
