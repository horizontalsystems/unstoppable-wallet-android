package io.horizontalsystems.bankwallet.modules.swap.oneinch

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.OneInchSwapSettings
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.oneinchkit.Quote
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

class OneInchTradeService(
    private val evmKit: EthereumKit,
    private val oneInchKitHelper: OneInchKitHelper
) : SwapMainModule.ISwapTradeService {

    private var quoteDisposable: Disposable? = null
    private var lastBlockDisposable: Disposable? = null

    //region internal subjects
    private val amountTypeSubject = PublishSubject.create<AmountType>()
    private val tokenFromSubject = PublishSubject.create<Optional<Token>>()
    private val tokenToSubject = PublishSubject.create<Optional<Token>>()
    private val amountFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val amountToSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val stateSubject = PublishSubject.create<State>()
    //endregion

    //region outputs
    override var tokenFrom: Token? = null
        private set(value) {
            field = value
            tokenFromSubject.onNext(Optional.ofNullable(value))
        }
    override val tokenFromObservable: Observable<Optional<Token>> = tokenFromSubject

    override var tokenTo: Token? = null
        private set(value) {
            field = value
            tokenToSubject.onNext(Optional.ofNullable(value))
        }
    override val tokenToObservable: Observable<Optional<Token>> = tokenToSubject

    override var amountFrom: BigDecimal? = null
        private set(value) {
            field = value
            amountFromSubject.onNext(Optional.ofNullable(value))
        }
    override val amountFromObservable: Observable<Optional<BigDecimal>> = amountFromSubject

    override var amountTo: BigDecimal? = null
        private set(value) {
            field = value
            amountToSubject.onNext(Optional.ofNullable(value))
        }
    override val amountToObservable: Observable<Optional<BigDecimal>> = amountToSubject

    override var amountType: AmountType = AmountType.ExactFrom
        private set(value) {
            field = value
            amountTypeSubject.onNext(value)
        }
    override val amountTypeObservable: Observable<AmountType> = amountTypeSubject

    var state: State = State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Observable<State> = stateSubject

    var swapSettings: OneInchSwapSettings = OneInchSwapSettings()
        set(value) {
            field = value
            syncQuote()
        }

    override fun enterTokenFrom(token: Token?) {
        if (tokenFrom == token) return

        tokenFrom = token

        if (amountType == AmountType.ExactTo) {
            amountFrom = null
        }

        if (tokenTo == tokenFrom) {
            tokenTo = null
            amountTo = null
        }

        syncQuote()
    }

    override fun enterTokenTo(token: Token?) {
        if (tokenTo == token) return

        tokenTo = token

        if (amountType == AmountType.ExactFrom) {
            amountTo = null
        }

        if (tokenFrom == tokenTo) {
            tokenFrom = null
            amountFrom = null
        }

        syncQuote()
    }

    override fun enterAmountFrom(amount: BigDecimal?) {
        amountType = AmountType.ExactFrom

        if (amountsEqual(amountFrom, amount)) return

        amountFrom = amount
        amountTo = null

        syncQuote()
    }

    override fun enterAmountTo(amount: BigDecimal?) {
        amountType = AmountType.ExactTo

        if (amountsEqual(amountTo, amount)) return

        amountTo = amount
        amountFrom = null

        syncQuote()
    }

    override fun switchCoins() {
        val swapCoin = tokenTo
        tokenTo = tokenFrom

        enterTokenFrom(swapCoin)
    }

    override fun restoreState(swapProviderState: SwapMainModule.SwapProviderState) {
        tokenFrom = swapProviderState.tokenFrom
        tokenTo = swapProviderState.tokenTo
        amountType = swapProviderState.amountType

        when (swapProviderState.amountType) {
            AmountType.ExactFrom -> {
                amountFrom = swapProviderState.amountFrom
                amountTo = null
            }
            AmountType.ExactTo -> {
                amountTo = swapProviderState.amountTo
                amountFrom = null
            }
        }

        syncQuote()
    }

    fun start() {
        lastBlockDisposable = evmKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                syncQuote()
            }
    }

    fun stop() {
        clearDisposables()
    }

    fun onCleared() {
        clearDisposables()
    }
    //endregion

    private fun clearDisposables() {
        lastBlockDisposable?.dispose()
        lastBlockDisposable = null

        quoteDisposable?.dispose()
        quoteDisposable = null
    }

    private fun syncQuote() {
        quoteDisposable?.dispose()
        quoteDisposable = null

        val amountFrom = amountFrom
        val tokenFrom = tokenFrom ?: return
        val tokenTo = tokenTo ?: return

        if (amountFrom == null || amountFrom.compareTo(BigDecimal.ZERO) == 0) {
            state = State.NotReady()
            return
        }

        state = State.Loading

        quoteDisposable = oneInchKitHelper.getQuoteAsync(tokenFrom, tokenTo, amountFrom)
            .subscribeIO({ quote ->
                handle(quote, tokenFrom, tokenTo, amountFrom)
            }, { error ->
                state = State.NotReady(listOf(error))
            })
    }

    private fun handle(quote: Quote, tokenFrom: Token, tokenTo: Token, amountFrom: BigDecimal) {
        val amountToBigDecimal =
            quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals)
                .stripTrailingZeros()

        amountTo = amountToBigDecimal

        val parameters = OneInchSwapParameters(
            tokenFrom,
            tokenTo,
            amountFrom,
            amountToBigDecimal,
            swapSettings.slippage,
            swapSettings.recipient
        )

        state = State.Ready(parameters)
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    //region models
    sealed class State {
        object Loading : State()
        class Ready(val params: OneInchSwapParameters) : State()
        class NotReady(val errors: List<Throwable> = listOf()) : State()
    }
    //endregion

}
