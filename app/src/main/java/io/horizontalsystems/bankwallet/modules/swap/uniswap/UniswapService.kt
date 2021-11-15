package io.horizontalsystems.bankwallet.modules.swap.uniswap

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

class UniswapService(
        private val dex: SwapMainModule.Dex,
        private val tradeService: UniswapTradeService,
        val allowanceService: SwapAllowanceService,
        val pendingAllowanceService: SwapPendingAllowanceService,
        private val adapterManager: IAdapterManager
) : SwapMainModule.ISwapService {
    private val disposables = CompositeDisposable()

    //region internal subjects
    private val stateSubject = PublishSubject.create<State>()
    private val errorsSubject = PublishSubject.create<List<Throwable>>()
    private val balanceFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val balanceToSubject = PublishSubject.create<Optional<BigDecimal>>()
    //endregion

    //region outputs
    var state: State = State.NotReady
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Observable<State> = stateSubject

    override var errors: List<Throwable> = listOf()
        private set(value) {
            field = value
            errorsSubject.onNext(value)
        }
    override val errorsObservable: Observable<List<Throwable>> = errorsSubject

    override var balanceFrom: BigDecimal? = null
        private set(value) {
            field = value
            balanceFromSubject.onNext(Optional.ofNullable(value))
        }
    override val balanceFromObservable: Observable<Optional<BigDecimal>> = balanceFromSubject

    override var balanceTo: BigDecimal? = null
        private set(value) {
            field = value
            balanceToSubject.onNext(Optional.ofNullable(value))
        }
    override val balanceToObservable: Observable<Optional<BigDecimal>> = balanceToSubject

    val approveData: SwapAllowanceService.ApproveData?
        get() = balanceFrom?.let { amount ->
            allowanceService.approveData(dex, amount)
        }
    //endregion

    init {
        tradeService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { state ->
                    onUpdateTrade(state)
                }
                .let { disposables.add(it) }

        tradeService.coinFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    onUpdateCoinFrom(coin.orElse(null))
                }
                .let { disposables.add(it) }
        onUpdateCoinFrom(tradeService.coinFrom)

        tradeService.coinToObservable
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    onUpdateCoinTo(coin.orElse(null))
                }
                .let { disposables.add(it) }

        tradeService.amountFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateAmountFrom(it.orElse(null))
                }
                .let { disposables.add(it) }

        allowanceService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncState()
                }
                .let { disposables.add(it) }

        pendingAllowanceService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateAllowancePending()
                }
                .let { disposables.add(it) }
    }

    override fun start() {
        allowanceService.start()
        tradeService.start()
    }

    override fun stop() {
        allowanceService.stop()
        tradeService.stop()
    }

    fun onCleared() {
        disposables.clear()
        tradeService.onCleared()
        allowanceService.onCleared()
        pendingAllowanceService.onCleared()
    }

    private fun onUpdateTrade(state: UniswapTradeService.State) {
        syncState()
    }

    private fun onUpdateCoinFrom(coin: PlatformCoin?) {
        balanceFrom = coin?.let { balance(it) }
        allowanceService.set(coin)
        pendingAllowanceService.set(coin)
    }

    private fun onUpdateCoinTo(coin: PlatformCoin?) {
        balanceTo = coin?.let { balance(it) }
    }

    private fun onUpdateAmountFrom(amount: BigDecimal?) {
        syncState()
    }

    private fun onUpdateAllowancePending() {
        syncState()
    }

    private fun syncState() {
        val allErrors = mutableListOf<Throwable>()
        var loading = false
        var transactionData: TransactionData? = null

        when (val state = tradeService.state) {
            UniswapTradeService.State.Loading -> {
                loading = true
            }
            is UniswapTradeService.State.Ready -> {
                transactionData = try {
                    tradeService.transactionData(state.trade.tradeData)
                } catch (error: Throwable) {
                    null
                }
            }
            is UniswapTradeService.State.NotReady -> {
                allErrors.addAll(state.errors)
            }
        }

        when (val state = allowanceService.state) {
            SwapAllowanceService.State.Loading -> {
                loading = true
            }
            is SwapAllowanceService.State.Ready -> {
                tradeService.amountFrom?.let { amountFrom ->
                    if (amountFrom > state.allowance.value) {
                        allErrors.add(SwapError.InsufficientAllowance)
                    }
                }
            }
            is SwapAllowanceService.State.NotReady -> {
                allErrors.add(state.error)
            }
        }

        tradeService.amountFrom?.let { amountFrom ->
            val balanceFrom = balanceFrom
            if (balanceFrom == null || balanceFrom < amountFrom) {
                allErrors.add(SwapError.InsufficientBalanceFrom)
            }
        }

        if (pendingAllowanceService.state == SwapPendingAllowanceState.Pending) {
            loading = true
        }

        errors = allErrors

        state = when {
            loading -> State.Loading
            errors.isEmpty() && transactionData != null -> State.Ready(transactionData)
            else -> State.NotReady
        }
    }

    private fun balance(coin: PlatformCoin): BigDecimal? =
            (adapterManager.getAdapterForPlatformCoin(coin) as? IBalanceAdapter)?.balanceData?.available

    //region models
    sealed class State {
        object Loading : State()
        class Ready(val transactionData: TransactionData) : State()
        object NotReady : State()
    }
    //endregion

}
