package io.horizontalsystems.bankwallet.modules.swap_new

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class SwapService(
        private val ethereumKit: EthereumKit,
        private val tradeService: SwapTradeService,
        private val adapterManager: IAdapterManager
) {

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
    //endregion


    init {
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
    }

    private fun onUpdateCoinFrom(coin: Coin?) {
        balanceFrom = coin?.let { balance(it) }
    }

    private fun onUpdateCoinTo(coin: Coin?) {
        balanceTo = coin?.let { balance(it) }
    }

    private fun balance(coin: Coin): BigDecimal? =
            (adapterManager.getAdapterForCoin(coin) as? IBalanceAdapter)?.balance

    //region models
    sealed class State {
        object Loading : State()
        object Ready : State()
        object NotReady : State()
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

}
