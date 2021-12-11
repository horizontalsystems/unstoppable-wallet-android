package io.horizontalsystems.bankwallet.modules.swap.uniswap

import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.bankwallet.modules.swap.providers.UniswapProvider
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*


class UniswapTradeService(
    private val evmKit: EthereumKit,
    private val uniswapProvider: UniswapProvider
) : SwapMainModule.ISwapTradeService {

    private var swapDataDisposable: Disposable? = null
    private var lastBlockDisposable: Disposable? = null
    private var swapData: SwapData? = null

    //region internal subjects
    private val amountTypeSubject = PublishSubject.create<AmountType>()
    private val coinFromSubject = PublishSubject.create<Optional<PlatformCoin>>()
    private val coinToSubject = PublishSubject.create<Optional<PlatformCoin>>()
    private val amountFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val amountToSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val stateSubject = PublishSubject.create<State>()
    private val tradeOptionsSubject = PublishSubject.create<SwapTradeOptions>()
    //endregion

    //region outputs
    override var coinFrom: PlatformCoin? = null
        private set(value) {
            field = value
            coinFromSubject.onNext(Optional.ofNullable(value))
        }
    override val coinFromObservable: Observable<Optional<PlatformCoin>> = coinFromSubject

    override var coinTo: PlatformCoin? = null
        private set(value) {
            field = value
            coinToSubject.onNext(Optional.ofNullable(value))
        }
    override val coinToObservable: Observable<Optional<PlatformCoin>> = coinToSubject

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

    var tradeOptions: SwapTradeOptions = SwapTradeOptions()
        set(value) {
            field = value
            tradeOptionsSubject.onNext(value)
            syncTradeData()
        }

    val tradeOptionsObservable: Observable<SwapTradeOptions> = tradeOptionsSubject

    @Throws
    fun transactionData(tradeData: TradeData): TransactionData {
        return uniswapProvider.transactionData(tradeData)
    }

    override fun enterCoinFrom(coin: PlatformCoin?) {
        if (coinFrom == coin) return

        coinFrom = coin

        if (amountType == AmountType.ExactTo) {
            amountFrom = null
        }

        if (coinTo == coinFrom) {
            coinTo = null
            amountTo = null
        }

        swapData = null
        syncSwapData()
    }

    override fun enterCoinTo(coin: PlatformCoin?) {
        if (coinTo == coin) return

        coinTo = coin

        if (amountType == AmountType.ExactFrom) {
            amountTo = null
        }

        if (coinFrom == coinTo) {
            coinFrom = null
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
        coinTo = swapProviderState.coinTo
        coinFrom = swapProviderState.coinFrom
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
        val swapCoin = coinTo
        coinTo = coinFrom

        enterCoinFrom(swapCoin)
    }

    fun start() {
        lastBlockDisposable = evmKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                syncSwapData()
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

        swapDataDisposable?.dispose()
        swapDataDisposable = null
    }

    private fun syncSwapData() {
        val coinFrom = coinFrom
        val coinTo = coinTo

        if (coinFrom == null || coinTo == null) {
            state = State.NotReady()
            return
        }

        if (swapData == null) {
            state = State.Loading
        }

        swapDataDisposable?.dispose()
        swapDataDisposable = null

        swapDataDisposable = uniswapProvider.swapDataSingle(coinFrom, coinTo)
            .subscribeOn(Schedulers.io())
            .subscribe({
                swapData = it
                syncTradeData()
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
            val tradeData =
                uniswapProvider.tradeData(swapData, amount, tradeType, tradeOptions.tradeOptions)
            handle(tradeData)
        } catch (e: Throwable) {
            state = State.NotReady(listOf(e))
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
