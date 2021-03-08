package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService.PriceImpactLevel
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class SwapService(
        val dex: SwapModule.Dex,
        private val ethereumKit: EthereumKit,
        private val tradeService: SwapTradeService,
        private val allowanceService: SwapAllowanceService,
        private val pendingAllowanceService: SwapPendingAllowanceService,
        private val transactionService: EvmTransactionService,
        private val adapterManager: IAdapterManager,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager
) {

    private val disposables = CompositeDisposable()
    private val ethereumBalance: BigInteger
        get() = ethereumKit.accountState?.balance ?: BigInteger.ZERO

    //region internal subjects
    private val stateSubject = PublishSubject.create<State>()
    private val swapEventSubject = PublishSubject.create<SwapEvent>()
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
    val swapEventObservable: Observable<SwapEvent> = swapEventSubject

    var errors: List<Throwable> = listOf()
        private set(value) {
            field = value
            errorsSubject.onNext(value)
        }
    val errorsObservable: Observable<List<Throwable>> = errorsSubject

    var balanceFrom: BigDecimal? = null
        private set(value) {
            field = value
            balanceFromSubject.onNext(Optional.ofNullable(value))
        }
    val balanceFromObservable: Observable<Optional<BigDecimal>> = balanceFromSubject

    var balanceTo: BigDecimal? = null
        private set(value) {
            field = value
            balanceToSubject.onNext(Optional.ofNullable(value))
        }
    val balanceToObservable: Observable<Optional<BigDecimal>> = balanceToSubject

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

        pendingAllowanceService.isPendingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateAllowancePending(it)
                }
                .let { disposables.add(it) }

        transactionService.transactionStatusObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncState()
                }
                .let { disposables.add(it) }
    }

    fun swap() {

        if (state != State.Ready) {
            return
        }

        val transaction = transactionService.transactionStatus.dataOrNull ?: return

        swapEventSubject.onNext(SwapEvent.Swapping)

        ethereumKit.send(
                transaction.data.to,
                transaction.data.value,
                transaction.data.input,
                transaction.gasData.gasPrice,
                transaction.gasData.gasLimit
        )
                .subscribeOn(Schedulers.io())
                .doOnSuccess {
                    enableCoinIfNotEnabled(tradeService.coinFrom)
                    enableCoinIfNotEnabled(tradeService.coinTo)
                }
                .subscribe({
                    swapEventSubject.onNext(SwapEvent.Completed)
                }, {
                    swapEventSubject.onNext(SwapEvent.Failed(it))
                })
                .let { disposables.add(it) }
    }

    fun onCleared() {
        disposables.clear()
        tradeService.onCleared()
        allowanceService.onCleared()
        pendingAllowanceService.onCleared()
        transactionService.onCleared()
    }

    private fun enableCoinIfNotEnabled(coin: Coin?) {
        if (coin == null) return

        val wallet = walletManager.wallet(coin)
        if (wallet != null) return

        val account = accountManager.account(coin.type) ?: return
        walletManager.save(listOf(Wallet(coin, account)))
    }

    private fun onUpdateTrade(state: SwapTradeService.State) {
        transactionService.transactionData = when (state) {
            is SwapTradeService.State.Ready -> {
                try {
                    tradeService.transactionData(state.trade.tradeData)
                } catch (error: Throwable) {
                    null
                }
            }
            else -> {
                null
            }
        }
        syncState()
    }

    private fun onUpdateCoinFrom(coin: Coin?) {
        balanceFrom = coin?.let { balance(it) }
        allowanceService.set(coin)
        pendingAllowanceService.set(coin)
    }

    private fun onUpdateCoinTo(coin: Coin?) {
        balanceTo = coin?.let { balance(it) }
    }

    private fun onUpdateAmountFrom(amount: BigDecimal?) {
        syncState()
    }

    private fun onUpdateAllowancePending(isPending: Boolean) {
        syncState()

        if (transactionService.transactionStatus is DataState.Error && !isPending) {
            transactionService.resync() // after required allowance is approved, transaction service state should be resynced
        }
    }

    private fun syncState() {
        val allErrors = mutableListOf<Throwable>()
        var loading = false

        when (val state = tradeService.state) {
            SwapTradeService.State.Loading -> {
                loading = true
            }
            is SwapTradeService.State.Ready -> {
                if (state.trade.priceImpactLevel == PriceImpactLevel.Forbidden) {
                    allErrors.add(SwapError.ForbiddenPriceImpactLevel)
                }
            }
            is SwapTradeService.State.NotReady -> {
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

        when (val state = transactionService.transactionStatus) {
            DataState.Loading -> {
                loading = true
            }
            is DataState.Success -> {
                val transaction = state.data
                if (transaction.totalAmount > ethereumBalance) {
                    allErrors.add(TransactionError.InsufficientBalance(transaction.totalAmount))
                }
            }
            is DataState.Error -> {
                if (!allErrors.any { it is SwapError || it is TransactionError }) {
                    allErrors.add(state.error)
                }
            }
        }

        if (pendingAllowanceService.isPending) {
            loading = true
        }

        errors = allErrors

        state = when {
            loading -> State.Loading
            errors.isEmpty() -> State.Ready
            else -> State.NotReady
        }
    }

    private fun balance(coin: Coin): BigDecimal? =
            (adapterManager.getAdapterForCoin(coin) as? IBalanceAdapter)?.balance

    //region models
    sealed class State {
        object Loading : State()
        object Ready : State()
        object NotReady : State()
    }

    sealed class SwapEvent {
        object Swapping : SwapEvent()
        object Completed : SwapEvent()
        class Failed(val error: Throwable) : SwapEvent()
    }

    sealed class SwapError : Throwable() {
        object InsufficientBalanceFrom : SwapError()
        object InsufficientAllowance : SwapError()
        object ForbiddenPriceImpactLevel : SwapError()
    }

    sealed class TransactionError : Throwable() {
        class InsufficientBalance(val requiredBalance: BigInteger) : TransactionError()
    }
    //endregion

    companion object {
        val defaultSlippage = BigDecimal("0.5")
    }

}
