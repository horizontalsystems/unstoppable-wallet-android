package io.horizontalsystems.bankwallet.modules.swap.oneinch

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.AmountType
import io.horizontalsystems.bankwallet.modules.swap.settings.oneinch.OneInchSwapSettingsModule.OneInchSwapSettings
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.math.absoluteValue


class OneInchTradeService(
        evmKit: EthereumKit,
        private val oneInchKit: OneInchKit
) : SwapMainModule.ISwapTradeService {

    private var getQuoteDisposable: Disposable? = null
    private var getSwapDisposable: Disposable? = null
    private var lastBlockDisposable: Disposable? = null

    //region internal subjects
    private val amountTypeSubject = PublishSubject.create<AmountType>()
    private val coinFromSubject = PublishSubject.create<Optional<Coin>>()
    private val coinToSubject = PublishSubject.create<Optional<Coin>>()
    private val amountFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val amountToSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val stateSubject = PublishSubject.create<State>()
    //endregion

    init {
        lastBlockDisposable = evmKit.lastBlockHeightFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncSwap()
                }
    }

    //region outputs
    override var coinFrom: Coin? = null
        private set(value) {
            field = value
            coinFromSubject.onNext(Optional.ofNullable(value))
        }
    override val coinFromObservable: Observable<Optional<Coin>> = coinFromSubject

    override var coinTo: Coin? = null
        private set(value) {
            field = value
            coinToSubject.onNext(Optional.ofNullable(value))
        }
    override val coinToObservable: Observable<Optional<Coin>> = coinToSubject

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

    var swapSettings: OneInchSwapSettings = OneInchSwapSettings(gasPrice = 1200000, recipient = null) // TODO
        set(value) {
            field = value
            syncSwap()
        }

    fun getTransactionData(swap: Swap): TransactionData {
        return swap.transaction.let {
            TransactionData(it.to, it.value, it.data)
        }
    }

    override fun enterCoinFrom(coin: Coin?) {
        if (coinFrom == coin) return

        coinFrom = coin

        if (amountType == AmountType.ExactTo) {
            amountFrom = null
        }

        if (coinTo == coinFrom) {
            coinTo = null
            amountTo = null
        }

        syncSwap()
    }

    override fun enterCoinTo(coin: Coin?) {
        if (coinTo == coin) return

        coinTo = coin

        if (amountType == AmountType.ExactFrom) {
            amountTo = null
        }

        if (coinFrom == coinTo) {
            coinFrom = null
            amountFrom = null
        }

        syncSwap()
    }

    override fun enterAmountFrom(amount: BigDecimal?) {
        amountType = AmountType.ExactFrom

        if (amountsEqual(amountFrom, amount)) return

        amountFrom = amount
        amountTo = null
        syncSwap()
    }

    override fun enterAmountTo(amount: BigDecimal?) {
        amountType = AmountType.ExactTo

        if (amountsEqual(amountTo, amount)) return

        amountTo = amount
        amountFrom = null
        syncSwap()
    }

    override fun switchCoins() {
        val swapCoin = coinTo
        coinTo = coinFrom

        enterCoinFrom(swapCoin)
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

        syncSwap()
    }

    fun onCleared() {
        lastBlockDisposable?.dispose()
    }
    //endregion

    private fun syncSwap() {
        val amountFrom = amountFrom ?: return
        val coinFrom = coinFrom ?: return
        val coinTo = coinTo ?: return

        state = State.Loading

        getSwapDisposable = oneInchKit.getSwapAsync(getCoinAddress(coinFrom), getCoinAddress(coinTo), getRawAmount(coinFrom, amountFrom), swapSettings.slippage.toFloat())
                .subscribeIO({ quote ->
                    handle(quote)
                }, { error ->
                    state = State.NotReady(listOf(error))
                })
    }

    private fun handle(swap: Swap) {
        val amountToBigDecimal = swap.toTokenAmount.abs().toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros()

        amountTo = amountToBigDecimal

        state = State.Ready(swap)
    }

//    private fun syncQuote() {
//        val amountFrom = amountFrom ?: return
//        val coinFrom = coinFrom ?: return
//        val coinTo = coinTo ?: return
//
//
//        val amountFromBigInteger = getRawAmount(coinFrom, amountFrom)
//        getQuoteDisposable = oneInchKit.getQuoteAsync(getCoinAddress(coinFrom), getCoinAddress(coinTo), amountFromBigInteger)
//                .subscribeIO({ quote ->
//                    handle(quote)
//                }, { error ->
//                    state = State.NotReady(listOf(error))
//                })
//    }

    private fun getCoinAddress(coin: Coin): Address {
        return when (val coinType = coin.type) {
            CoinType.Ethereum, CoinType.BinanceSmartChain -> Address("0xeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
            is CoinType.Erc20 -> Address(coinType.address)
            is CoinType.Bep20 -> Address(coinType.address)
            else -> throw IllegalStateException("Unsupported coinType: $coinType")
        }
    }

    private fun getRawAmount(coin: Coin, decimal: BigDecimal): BigInteger {
        val exponent = coin.decimal - decimal.scale()

        return if (exponent >= 0) {
            decimal.unscaledValue() * BigInteger.TEN.pow(exponent)
        } else {
            decimal.unscaledValue() / BigInteger.TEN.pow(exponent.absoluteValue)
        }
    }

//    private fun handle(quote: Quote) {
//        val amountToBigDecimal = quote.toTokenAmount.abs().toBigDecimal().movePointLeft(quote.toToken.decimals).stripTrailingZeros()
//
//        amountTo = amountToBigDecimal
//
//        state = State.Ready(quote)
//    }

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
        class Ready(val swap: Swap) : State()
        class NotReady(val errors: List<Throwable> = listOf()) : State()
    }
    //endregion

}
