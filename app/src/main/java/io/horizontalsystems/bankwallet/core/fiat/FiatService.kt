package io.horizontalsystems.bankwallet.core.fiat

import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService.AmountType
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
import java.math.RoundingMode

class FiatService(
    private val switchService: AmountTypeSwitchService,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) : AmountTypeSwitchService.IToggleAvailableListener {

    private val disposables = CompositeDisposable()
    private var latestRateDisposable: Disposable? = null

    private var token: Token? = null
    private var coinAmount: BigDecimal? = null
    private var currencyAmount: BigDecimal? = null

    private val toggleAvailableSubject = PublishSubject.create<Boolean>()
    override var toggleAvailable: Boolean = false
        private set(value) {
            field = value
            toggleAvailableSubject.onNext(value)
        }
    override val toggleAvailableObservable: Observable<Boolean>
        get() = toggleAvailableSubject

    private var rate: BigDecimal? = null

    val currency: Currency
        get() = currencyManager.baseCurrency

    private val _fullAmountInfoFlow =
        MutableSharedFlow<FullAmountInfo?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val fullAmountInfoFlow = _fullAmountInfoFlow.asSharedFlow()

    init {
        switchService.amountTypeObservable
            .subscribeOn(Schedulers.io())
            .subscribe {
                syncAmountType()
            }
            .let { disposables.add(it) }
    }

    private fun subscribeToLatestRate() {
        latestRateDisposable?.dispose()
        latestRateDisposable = null

        toggleAvailable = false

        val token = token ?: return

        syncLatestRate(marketKit.coinPrice(token.coin.uid, currency.code))

        latestRateDisposable = marketKit.coinPriceObservable("fiat-service", token.coin.uid, currency.code)
            .distinct()
            .subscribeOn(Schedulers.io())
            .subscribe {
                syncLatestRate(it)
            }
    }

    private fun syncLatestRate(coinPrice: CoinPrice?) {
        rate = if (coinPrice != null && !coinPrice.expired) {
            coinPrice.value
        } else {
            null
        }

        toggleAvailable = rate != null

        _fullAmountInfoFlow.tryEmit(fullAmountInfo())
    }

    private fun fullAmountInfo(): FullAmountInfo? {
        val token = token ?: return null
        val coinAmount = coinAmount ?: return null

        return when (switchService.amountType) {
            AmountType.Coin -> {
                val primary = CoinValue(token, coinAmount)
                val secondary = currencyAmount?.let { CurrencyValue(currency, it) }
                FullAmountInfo(
                    primaryInfo = AmountInfo.CoinValueInfo(primary),
                    secondaryInfo = secondary?.let { AmountInfo.CurrencyValueInfo(secondary) },
                    coinValue = primary
                )
            }

            AmountType.Currency -> {
                val currencyAmount = currencyAmount ?: return null

                val primary = CurrencyValue(currency, currencyAmount)
                val secondary = CoinValue(token, coinAmount)
                FullAmountInfo(
                    primaryInfo = AmountInfo.CurrencyValueInfo(primary),
                    secondaryInfo = AmountInfo.CoinValueInfo(secondary),
                    coinValue = secondary
                )
            }
        }
    }

    private fun syncAmountType() {
        _fullAmountInfoFlow.tryEmit(fullAmountInfo())
    }

    fun buildForCoin(amount: BigDecimal?): FullAmountInfo? {
        coinAmount = amount

        currencyAmount = amount?.let { coinAmount ->
            rate?.let { rate ->
                coinAmount * rate
            }
        }

        return fullAmountInfo()
    }

    fun buildForCurrency(amount: BigDecimal?): FullAmountInfo? {
        val coin = token ?: return null

        currencyAmount = amount

        coinAmount = amount?.let { currencyAmount ->
            rate?.let { rate ->
                if (rate.compareTo(BigDecimal.ZERO) == 0)
                    BigDecimal.ZERO
                else
                    currencyAmount.divide(rate, coin.decimals, RoundingMode.FLOOR)
            }
        }
        return fullAmountInfo()
    }

    fun buildAmountInfo(amount: BigDecimal?): FullAmountInfo? {
        return when (switchService.amountType) {
            AmountType.Coin -> buildForCoin(amount)
            AmountType.Currency -> buildForCurrency(amount)
        }
    }

    fun set(token: Token?) {
        this.token = token

        rate = null
        subscribeToLatestRate()

        when (switchService.amountType) {
            AmountType.Coin -> {
                _fullAmountInfoFlow.tryEmit(buildForCoin(coinAmount))
            }

            AmountType.Currency -> {
                _fullAmountInfoFlow.tryEmit(buildForCurrency(currencyAmount))
            }
        }
    }

    data class FullAmountInfo(
        val primaryInfo: AmountInfo,
        val secondaryInfo: AmountInfo?,
        val coinValue: CoinValue
    ) {
        val primaryValue: BigDecimal
            get() = when (primaryInfo) {
                is AmountInfo.CoinValueInfo -> primaryInfo.coinValue.value
                is AmountInfo.CurrencyValueInfo -> primaryInfo.currencyValue.value
            }

        val primaryDecimal: Int
            get() = when (primaryInfo) {
                is AmountInfo.CoinValueInfo -> primaryInfo.coinValue.decimal
                is AmountInfo.CurrencyValueInfo -> primaryInfo.currencyValue.currency.decimal
            }
    }

}
