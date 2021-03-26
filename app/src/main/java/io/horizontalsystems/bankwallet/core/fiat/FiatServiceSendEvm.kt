package io.horizontalsystems.bankwallet.core.fiat

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm.AmountType
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class FiatServiceSendEvm(
        private val switchService: AmountTypeSwitchServiceSendEvm,
        private val currencyManager: ICurrencyManager,
        private val rateManager: IRateManager
) : Clearable {

    private val disposable = CompositeDisposable()
    private var marketInfoDisposable: Disposable? = null

    private var coin: Coin? = null

    private var toggleAvailableSubject = PublishSubject.create<Boolean>()
    val toggleAvailableObservable: Flowable<Boolean>
        get() = toggleAvailableSubject.toFlowable(BackpressureStrategy.BUFFER)

    private var rate: BigDecimal? = null
        set(value) {
            field = value
            toggleAvailableSubject.onNext(rate != null)
        }

    private var coinAmountSubject = PublishSubject.create<BigDecimal>()
    val coinAmountObservable: Flowable<BigDecimal>
        get() = coinAmountSubject.toFlowable(BackpressureStrategy.BUFFER)

    private var _coinAmount: BigDecimal = BigDecimal.ZERO
        set(value) {
            if (field.compareTo(value) != 0) {
                field = value
                coinAmountSubject.onNext(value)
            }
        }
    val coinAmount: BigDecimal
        get() = _coinAmount

    private var currencyAmount: BigDecimal? = null

    private val primaryInfoSubject = PublishSubject.create<PrimaryInfo>()
    var primaryInfo: PrimaryInfo = PrimaryInfo.Amount(BigDecimal.ZERO)
        private set(value) {
            field = value
            primaryInfoSubject.onNext(value)
        }
    val primaryInfoObservable: Flowable<PrimaryInfo>
        get() = primaryInfoSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val secondaryAmountInfoSubject = PublishSubject.create<Optional<SendModule.AmountInfo>>()
    var secondaryAmountInfo: SendModule.AmountInfo? = null
        private set(value) {
            field = value
            secondaryAmountInfoSubject.onNext(Optional.ofNullable(value))
        }
    val secondaryAmountInfoObservable: Flowable<Optional<SendModule.AmountInfo>>
        get() = secondaryAmountInfoSubject.toFlowable(BackpressureStrategy.BUFFER)

    val currency = currencyManager.baseCurrency

    var coinAmountLocked = false

    init {
        switchService.amountTypeObservable
                .subscribeIO {
                    sync()
                }.let {
                    disposable.add(it)
                }

        sync()
    }

    private fun syncLatestRate(latestRate: LatestRate?) {
        if (latestRate != null && !latestRate.isExpired()) {
            rate = latestRate.rate
            if (coinAmountLocked) {
                syncCurrencyAmount()
            } else {
                when (switchService.amountType) {
                    AmountType.Coin -> syncCurrencyAmount()
                    AmountType.Currency -> syncCoinAmount()
                }
            }
        } else {
            rate = null
        }

        sync()
    }

    private fun sync() {
        val coin = coin
        if (coin != null) {
            val coinAmountInfo = SendModule.AmountInfo.CoinValueInfo(CoinValue(coin, coinAmount))
            val currencyAmountInfo = currencyAmount?.let { SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currency, it)) }

            when (switchService.amountType) {
                AmountType.Coin -> {
                    primaryInfo = PrimaryInfo.Info(coinAmountInfo)
                    secondaryAmountInfo = currencyAmountInfo
                }
                AmountType.Currency -> {
                    primaryInfo = PrimaryInfo.Info(currencyAmountInfo)
                    secondaryAmountInfo = coinAmountInfo
                }
            }
        } else {
            primaryInfo = PrimaryInfo.Amount(coinAmount)
            secondaryAmountInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currency, BigDecimal.ZERO))
        }
    }

    private fun syncCoinAmount() {
        val currencyAmount = currencyAmount
        val rate = rate
        val coin = coin

        _coinAmount = if (coin != null && currencyAmount != null && rate != null && rate > BigDecimal.ZERO) {
            currencyAmount.divide(rate, coin.decimal, RoundingMode.FLOOR)
        } else {
            BigDecimal.ZERO
        }
    }

    private fun syncCurrencyAmount() {
        currencyAmount = rate?.let { coinAmount * it }
    }

    fun setCoin(coin: Coin?) {
        this.coin = coin

        marketInfoDisposable?.dispose()
        marketInfoDisposable = null

        if (coin != null) {
            syncLatestRate(rateManager.latestRate(coin.type, currency.code))

            rateManager.latestRateObservable(coin.type, currency.code)
                    .subscribeIO { latestRate ->
                        syncLatestRate(latestRate)
                    }.let { disposable.add(it) }
        } else {
            rate = null
            currencyAmount = null
            sync()
        }
    }

    fun setAmount(amount: BigDecimal) {
        when (switchService.amountType) {
            AmountType.Coin -> {
                _coinAmount = amount
                syncCurrencyAmount()
            }
            AmountType.Currency -> {
                currencyAmount = amount
                syncCoinAmount()
            }
        }
        sync()
    }

    fun setCoinAmount(amount: BigDecimal) {
        _coinAmount = amount
        syncCurrencyAmount()
        sync()
    }

    override fun clear() {
        disposable.clear()
    }

    sealed class PrimaryInfo {
        class Info(val amountInfo: SendModule.AmountInfo?) : PrimaryInfo()
        class Amount(val amount: BigDecimal) : PrimaryInfo()
    }

}
