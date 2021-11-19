package io.horizontalsystems.bankwallet.core.fiat

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm.AmountType
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode

class FiatServiceSendEvm(
        private val switchService: AmountTypeSwitchServiceSendEvm,
        private val currencyManager: ICurrencyManager,
        private val marketKit: MarketKit
) : Clearable {

    private val disposable = CompositeDisposable()
    private var marketInfoDisposable: Disposable? = null

    private var coin: PlatformCoin? = null

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

    private val inputsUpdatedSubject = PublishSubject.create<Unit>()
    val inputsUpdatedObservable: Flowable<Unit>
        get() = inputsUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    var primaryInfo: PrimaryInfo = PrimaryInfo.Amount(BigDecimal.ZERO)
        private set

    var secondaryAmountInfo: SendModule.AmountInfo? = null
        private set

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

    private fun syncLatestRate(latestRate: CoinPrice?) {
        if (latestRate != null && !latestRate.expired) {
            rate = latestRate.value
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
            val coinAmountInfo = SendModule.AmountInfo.CoinValueInfo(CoinValue(CoinValue.Kind.PlatformCoin(coin), coinAmount))
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
        inputsUpdatedSubject.onNext(Unit)
    }

    private fun syncCoinAmount() {
        val currencyAmount = currencyAmount
        val rate = rate
        val coin = coin

        _coinAmount = if (coin != null && currencyAmount != null && rate != null && rate > BigDecimal.ZERO) {
            currencyAmount.divide(rate, coin.decimals, RoundingMode.FLOOR)
        } else {
            BigDecimal.ZERO
        }
    }

    private fun syncCurrencyAmount() {
        currencyAmount = rate?.let { coinAmount * it }
    }

    fun setCoin(platformCoin: PlatformCoin?) {
        this.coin = platformCoin

        marketInfoDisposable?.dispose()
        marketInfoDisposable = null

        if (platformCoin != null) {
            syncLatestRate(marketKit.coinPrice(platformCoin.coin.uid, currency.code))

            if (!platformCoin.coin.isCustom) {
                marketKit.coinPriceObservable(platformCoin.coin.uid, currency.code)
                    .subscribeIO { latestRate ->
                        syncLatestRate(latestRate)
                    }.let { disposable.add(it) }
            }
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
