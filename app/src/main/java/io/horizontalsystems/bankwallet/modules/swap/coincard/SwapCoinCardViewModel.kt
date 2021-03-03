package io.horizontalsystems.bankwallet.modules.swap.coincard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService.AmountType
import io.horizontalsystems.bankwallet.core.fiat.FiatService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

class SwapCoinCardViewModel(
        private val coinCardService: ISwapCoinCardService,
        private val fiatService: FiatService,
        private val switchService: AmountTypeSwitchService,
        private val maxButtonSupported: Boolean,
        private val formatter: SwapViewItemHelper,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val validDecimals: Int
        get() {
            val decimals = when (switchService.amountType) {
                AmountType.Coin -> coinCardService.coin?.decimal ?: maxValidDecimals
                AmountType.Currency -> fiatService.currency.decimal
            }
            return min(decimals, maxValidDecimals)
        }

    private val amountLiveData = MutableLiveData<String?>(null)
    private val balanceLiveData = MutableLiveData<String?>(null)
    private val balanceErrorLiveData = MutableLiveData(false)
    private val tokenCodeLiveData = MutableLiveData<String?>(null)
    private val isEstimatedLiveData = MutableLiveData(false)
    private val switchEnabledLiveData = MutableLiveData(false)
    private val prefixLiveData = MutableLiveData<String?>(null)
    private val secondaryInfoLiveData = MutableLiveData<String?>(null)
    private val maxEnabledLiveData = MutableLiveData(false)

    //region outputs
    fun amountLiveData(): LiveData<String?> = amountLiveData
    fun balanceLiveData(): LiveData<String?> = balanceLiveData
    fun balanceErrorLiveData(): LiveData<Boolean> = balanceErrorLiveData
    fun tokenCodeLiveData(): LiveData<String?> = tokenCodeLiveData
    fun isEstimatedLiveData(): LiveData<Boolean> = isEstimatedLiveData
    fun switchEnabledLiveData(): LiveData<Boolean> = switchEnabledLiveData
    fun prefixLiveData(): LiveData<String?> = prefixLiveData
    fun secondaryInfoLiveData(): LiveData<String?> = secondaryInfoLiveData
    fun maxEnabledLiveData(): LiveData<Boolean> = maxEnabledLiveData

    val tokensForSelection: List<SwapModule.CoinBalanceItem>
        get() = coinCardService.tokensForSelection

    fun onSelectCoin(coin: Coin) {
        coinCardService.onSelectCoin(coin)
        fiatService.set(coin)
    }

    fun onChangeAmount(amount: String?) {
        val validAmount = amount?.toBigDecimalOrNull()
        val fullAmountInfo = fiatService.buildAmountInfo(validAmount)

        syncFullAmountInfo(fullAmountInfo, true, validAmount)
    }

    fun isValid(amount: String?): Boolean {
        val newAmount = amount?.toBigDecimalOrNull()

        return when {
            amount.isNullOrBlank() -> true
            newAmount != null && newAmount.scale() > validDecimals -> false
            else -> true
        }
    }

    fun onSwitch() {
        switchService.toggle()
    }

    fun onTapMax() {
        val balance = coinCardService.balance ?: return

        val fullAmountInfo = fiatService.buildForCoin(balance)
        syncFullAmountInfo(fullAmountInfo, true, balance)
    }
    //endregion

    init {
        subscribeToServices()
    }

    private fun subscribeToServices() {
        syncEstimated(coinCardService.isEstimated)
        syncAmount(coinCardService.amount)
        syncCoin(coinCardService.coin)
        syncBalance(coinCardService.balance)
        syncFullAmountInfo(null, false)

        coinCardService.isEstimatedObservable
                .subscribeOn(Schedulers.io())
                .subscribe { syncEstimated(it) }
                .let { disposables.add(it) }

        coinCardService.amountObservable
                .subscribeOn(Schedulers.io())
                .subscribe { syncAmount(it.orElse(null)) }
                .let { disposables.add(it) }

        coinCardService.coinObservable
                .subscribeOn(Schedulers.io())
                .subscribe { syncCoin(it.orElse(null)) }
                .let { disposables.add(it) }

        coinCardService.balanceObservable
                .subscribeOn(Schedulers.io())
                .subscribe { syncBalance(it.orElse(null)) }
                .let { disposables.add(it) }

        coinCardService.errorObservable
                .subscribeOn(Schedulers.io())
                .subscribe { syncError(it.orElse(null)) }
                .let { disposables.add(it) }

        fiatService.fullAmountInfoObservable
                .subscribeOn(Schedulers.io())
                .subscribe { syncFullAmountInfo(it.orElse(null), false) }
                .let { disposables.add(it) }

        switchService.toggleAvailableObservable
                .subscribeOn(Schedulers.io())
                .subscribe { switchEnabledLiveData.postValue(it) }
                .let { disposables.add(it) }
    }

    private fun syncEstimated(estimated: Boolean) {
        isEstimatedLiveData.postValue(estimated)
    }

    private fun syncAmount(amount: BigDecimal?) {
        if (coinCardService.isEstimated) {
            val fullAmountInfo = fiatService.buildForCoin(amount)
            syncFullAmountInfo(fullAmountInfo, false)
        }
    }

    private fun syncCoin(coin: Coin?) {
        fiatService.set(coin)
        tokenCodeLiveData.postValue(coin?.code)
    }

    private fun syncBalance(balance: BigDecimal?) {
        val coin = coinCardService.coin
        val formattedBalance = when {
            coin == null -> stringProvider.string(R.string.NotAvailable)
            balance == null -> null
            else -> formatter.coinAmount(balance, coin)
        }
        balanceLiveData.postValue(formattedBalance)
        val balanceNonNull = balance ?: BigDecimal.ZERO
        maxEnabledLiveData.postValue(balanceNonNull > BigDecimal.ZERO && coin?.type != CoinType.Ethereum && coin?.type != CoinType.BinanceSmartChain && maxButtonSupported)
    }

    private fun syncError(error: Throwable?) {
        balanceErrorLiveData.postValue(error != null)
    }

    private fun secondaryInfoPlaceHolder(): String? = when (switchService.amountType) {
        AmountType.Coin -> {
            val amountInfo = AmountInfo.CurrencyValueInfo(CurrencyValue(fiatService.currency, BigDecimal.ZERO))
            amountInfo.getFormatted()
        }
        AmountType.Currency -> {
            val amountInfo = coinCardService.coin?.let { AmountInfo.CoinValueInfo(CoinValue(it, BigDecimal.ZERO)) }
            amountInfo?.getFormatted()
        }
    }

    private fun syncFullAmountInfo(fullAmountInfo: FiatService.FullAmountInfo?, force: Boolean = false, inputAmount: BigDecimal? = null) {
        prefixLiveData.postValue(if (switchService.amountType == AmountType.Currency) fiatService.currency.symbol else null)

        if (fullAmountInfo == null) {
            if (!force && coinCardService.isEstimated) {
                amountLiveData.postValue(null)
            }
            secondaryInfoLiveData.postValue(secondaryInfoPlaceHolder())

            setCoinValueToService(inputAmount, force)
        } else {
            val decimals = min(fullAmountInfo.primaryDecimal, maxValidDecimals)
            val amountString = fullAmountInfo.primaryValue.setScale(decimals, RoundingMode.FLOOR)?.stripTrailingZeros()?.toPlainString()
            amountLiveData.postValue(amountString)

            secondaryInfoLiveData.postValue(fullAmountInfo.secondaryInfo?.getFormatted())

            setCoinValueToService(fullAmountInfo.coinValue.value, force)
        }
    }

    private fun setCoinValueToService(coinAmount: BigDecimal?, force: Boolean) {
        if (force || !coinCardService.isEstimated) {
            coinCardService.onChangeAmount(coinAmount)
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    companion object {
        private const val maxValidDecimals = 8
    }

}
