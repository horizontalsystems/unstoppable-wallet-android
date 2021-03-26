package io.horizontalsystems.bankwallet.core.fiat

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService.AmountType
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class FiatService(
        private val switchService: AmountTypeSwitchService,
        private val currencyManager: ICurrencyManager,
        private val rateManager: IRateManager
) : AmountTypeSwitchService.IToggleAvailableListener {

    private val disposables = CompositeDisposable()
    private var latestRateDisposable: Disposable? = null

    private var coin: Coin? = null
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

    private val fullAmountInfoSubject = PublishSubject.create<Optional<FullAmountInfo>>()
    val fullAmountInfoObservable: Observable<Optional<FullAmountInfo>>
        get() = fullAmountInfoSubject

    init {
        switchService.amountTypeObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncAmountType(it)
                }
                .let { disposables.add(it) }
    }

    private fun subscribeToLatestRate() {
        latestRateDisposable?.dispose()
        latestRateDisposable = null

        toggleAvailable = false

        val coin = coin ?: return

        syncLatestRate(rateManager.latestRate(coin.type, currency.code))
        latestRateDisposable = rateManager.latestRateObservable(coin.type, currency.code)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncLatestRate(it)
                }
    }

    private fun syncLatestRate(latestRate: LatestRate?) {
        rate = if (latestRate != null && !latestRate.isExpired()) {
            latestRate.rate
        } else {
            null
        }

        toggleAvailable = rate != null

        fullAmountInfoSubject.onNext(Optional.ofNullable(fullAmountInfo()))
    }

    private fun fullAmountInfo(): FullAmountInfo? {
        val coin = coin ?: return null
        val coinAmount = coinAmount ?: return null

        return when (switchService.amountType) {
            AmountType.Coin -> {
                val primary = CoinValue(coin, coinAmount)
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
                val secondary = CoinValue(coin, coinAmount)
                FullAmountInfo(
                        primaryInfo = AmountInfo.CurrencyValueInfo(primary),
                        secondaryInfo = AmountInfo.CoinValueInfo(secondary),
                        coinValue = secondary
                )
            }
        }
    }

    private fun syncAmountType(amountType: AmountType) {
        fullAmountInfoSubject.onNext(Optional.ofNullable(fullAmountInfo()))
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
        val coin = coin ?: return null

        currencyAmount = amount

        coinAmount = amount?.let { currencyAmount ->
            rate?.let { rate ->
                if (rate.compareTo(BigDecimal.ZERO) == 0)
                    BigDecimal.ZERO
                else
                    currencyAmount.divide(rate, coin.decimal, RoundingMode.FLOOR)
            }
        }
        return fullAmountInfo()
    }

    fun buildAmountInfo(amount: BigDecimal?): FullAmountInfo? =
            when (switchService.amountType) {
                AmountType.Coin -> buildForCoin(amount)
                AmountType.Currency -> buildForCurrency(amount)
            }

    fun set(coin: Coin?) {
        this.coin = coin

        rate = null
        subscribeToLatestRate()

        when (switchService.amountType) {
            AmountType.Coin -> fullAmountInfoSubject.onNext(Optional.ofNullable(buildForCoin(coinAmount)))
            AmountType.Currency -> fullAmountInfoSubject.onNext(Optional.ofNullable(buildForCurrency(currencyAmount)))
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
                is AmountInfo.CoinValueInfo -> primaryInfo.coinValue.coin.decimal
                is AmountInfo.CurrencyValueInfo -> primaryInfo.currencyValue.currency.decimal
            }
    }

}
