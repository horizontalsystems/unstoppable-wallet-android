package io.horizontalsystems.bankwallet.modules.swap.uniswap

import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.bankwallet.modules.swap.providers.UniswapProvider
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.schedule


class UniswapTradeService(
    private val evmKit: EthereumKit,
    private val uniswapProvider: UniswapProvider
) : SwapMainModule.ISwapTradeService {

    private var swapDataDisposable: Disposable? = null
    private var lastBlockDisposable: Disposable? = null
    private var swapData: SwapData? = null
    private var timer: Timer? = null
    private val timeoutPeriodSeconds = evmKit.chain.syncInterval
    private val timeoutProgressStep = 1f / (timeoutPeriodSeconds * 2)

    //region internal subjects
    private val amountTypeSubject = PublishSubject.create<AmountType>()
    private val tokenFromSubject = PublishSubject.create<Optional<Token>>()
    private val tokenToSubject = PublishSubject.create<Optional<Token>>()
    private val amountFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val amountToSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val stateSubject = PublishSubject.create<State>()
    private val timeoutProgressSubject = BehaviorSubject.create<Float>()
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

    override val timeoutProgressObservable: Observable<Float>
        get() = timeoutProgressSubject

    var state: State = State.NotReady()
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Observable<State> = stateSubject

    var tradeOptions: SwapTradeOptions = SwapTradeOptions()
        set(value) {
            field = value
            syncTradeData()
        }

    @Throws
    fun transactionData(tradeData: TradeData): TransactionData {
        return uniswapProvider.transactionData(tradeData)
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

        swapData = null
        syncSwapData()
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

        swapData = null
        syncSwapData()
    }

    override fun enterAmountFrom(amount: BigDecimal?) {
        amountType = AmountType.ExactFrom

        if (amountsEqual(amountFrom, amount)) return

        amountFrom = amount
        amountTo = null
        syncTradeData()
    }

    override fun enterAmountTo(amount: BigDecimal?) {
        amountType = AmountType.ExactTo

        if (amountsEqual(amountTo, amount)) return

        amountTo = amount
        amountFrom = null
        syncTradeData()
    }

    override fun restoreState(swapProviderState: SwapMainModule.SwapProviderState) {
        tokenTo = swapProviderState.tokenTo
        tokenFrom = swapProviderState.tokenFrom
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

        swapData = null
        syncSwapData()
    }

    override fun switchCoins() {
        val swapCoin = tokenTo
        tokenTo = tokenFrom

        enterTokenFrom(swapCoin)
    }

    private fun startTimer() {
        timer = Timer().apply {
            schedule(0, 500) {
                onFireTimer()
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun resetTimer() {
        stopTimer()
        startTimer()
    }

    private fun onFireTimer() {
        val currentTimeoutProgress = timeoutProgressSubject.value ?: return
        val newTimeoutProgress = currentTimeoutProgress - timeoutProgressStep

        timeoutProgressSubject.onNext(newTimeoutProgress.coerceAtLeast(0f))
    }

    fun start() {
        sync()
        lastBlockDisposable = evmKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe { sync() }
    }

    private fun sync() {
        syncSwapData()
        resetTimer()
    }

    fun stop() {
        clearDisposables()
    }

    fun onCleared() {
        clearDisposables()
        stopTimer()
    }
    //endregion

    private fun clearDisposables() {
        lastBlockDisposable?.dispose()
        lastBlockDisposable = null

        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    private fun syncSwapData() {
        val tokenFrom = tokenFrom
        val tokenTo = tokenTo

        if (tokenFrom == null || tokenTo == null) {
            state = State.NotReady()
            return
        }

        state = State.Loading

        swapDataDisposable?.dispose()
        swapDataDisposable = null

        swapDataDisposable = uniswapProvider.swapDataSingle(tokenFrom, tokenTo)
            .subscribeOn(Schedulers.io())
            .subscribe({
                swapData = it
                syncTradeData()
                timeoutProgressSubject.onNext(1f)
            }, { error ->
                state = State.NotReady(listOf(error))
            })
    }

    private fun syncTradeData() {
        val swapData = swapData ?: return

        val amount = if (amountType == AmountType.ExactFrom) amountFrom else amountTo

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            state = State.NotReady()
            return
        }

        try {
            val tradeType = when (amountType) {
                AmountType.ExactFrom -> TradeType.ExactIn
                AmountType.ExactTo -> TradeType.ExactOut
            }
            val tradeData = uniswapProvider.tradeData(swapData, amount, tradeType, tradeOptions.tradeOptions)
            handle(tradeData)
        } catch (e: Throwable) {
            val error = when {
                e is TradeError.TradeNotFound && isEthWrapping(tokenFrom, tokenTo) -> TradeServiceError.WrapUnwrapNotAllowed
                else -> e
            }
            state = State.NotReady(listOf(error))
        }
    }

    private val wethAddressHex = uniswapProvider.wethAddress.hex
    private val Token.isWeth: Boolean
        get() = (type as? TokenType.Eip20)?.address?.equals(wethAddressHex, ignoreCase = true) ?: false
    private val Token.isNative: Boolean
        get() = type == TokenType.Native

    private fun isEthWrapping(tokenFrom: Token?, tokenTo: Token?) =
        when {
            tokenFrom == null || tokenTo == null -> false
            else -> {
                tokenFrom.isNative && tokenTo.isWeth || tokenTo.isNative && tokenFrom.isWeth
            }
        }

    private fun handle(tradeData: TradeData) {
        when (tradeData.type) {
            TradeType.ExactIn -> amountTo = tradeData.amountOut
            TradeType.ExactOut -> amountFrom = tradeData.amountIn
        }
        state = State.Ready(Trade(tradeData))
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
        class Ready(val trade: Trade) : State()
        class NotReady(val errors: List<Throwable> = listOf()) : State()
    }

    sealed class TradeServiceError : Throwable() {
        object WrapUnwrapNotAllowed : TradeServiceError()
    }

    enum class PriceImpactLevel {
        Normal, Warning, Forbidden
    }

    data class Trade(
        val tradeData: TradeData
    ) {
        val priceImpactLevel: PriceImpactLevel? = tradeData.priceImpact?.let {
            when {
                it >= BigDecimal.ZERO && it < warningPriceImpact -> PriceImpactLevel.Normal
                it >= warningPriceImpact && it < forbiddenPriceImpact -> PriceImpactLevel.Warning
                else -> PriceImpactLevel.Forbidden
            }
        }

    }
    //endregion

    companion object {
        private val warningPriceImpact = BigDecimal(1)
        private val forbiddenPriceImpact = BigDecimal(5)
    }

}
