package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchServiceSendEvm
import io.horizontalsystems.bankwallet.core.fiat.FiatServiceSendEvm
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import java.lang.Integer.min
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


interface IAmountInputService {
    val amount: BigDecimal
    val coin: Coin?
    val balance: BigDecimal?

    val amountObservable: Flowable<BigDecimal>
    val coinObservable: Flowable<Optional<Coin>>

    fun onChangeAmount(amount: BigDecimal)
}

class AmountInputViewModel(
        private val service: IAmountInputService,
        private val fiatService: FiatServiceSendEvm,
        private val switchService: AmountTypeSwitchServiceSendEvm,
        private val isMaxSupported: Boolean = true,
        private val clearables: List<Clearable>
) : ViewModel() {
    private val disposable = CompositeDisposable()

    private var coinDecimal = maxCoinDecimal

    private val validDecimal: Int
        get() = when (switchService.amountType) {
            AmountTypeSwitchServiceSendEvm.AmountType.Coin -> coinDecimal
            AmountTypeSwitchServiceSendEvm.AmountType.Currency -> fiatService.currency.decimal
        }

    val prefixLiveData = MutableLiveData<String?>(null)
    val amountLiveData = MutableLiveData<String?>(null)
    val maxEnabledLiveData = MutableLiveData(false)
    val secondaryTextLiveData = MutableLiveData<String?>(null)
    val switchEnabledLiveData = MutableLiveData(false)
    val revertAmountLiveData = MutableLiveData<String>()

    init {
        service.amountObservable.subscribeIO { syncAmount(it) }.let { disposable.add(it) }
        service.coinObservable.subscribeIO { syncCoin(it.orElse(null)) }.let { disposable.add(it) }
        fiatService.coinAmountObservable.subscribeIO { syncCoinAmount(it) }.let { disposable.add(it) }
        fiatService.primaryInfoObservable.subscribeIO { syncPrimaryInfo(it) }.let { disposable.add(it) }
        fiatService.secondaryAmountInfoObservable.subscribeIO { syncSecondaryAmountInfo(it.orElse(null)) }.let { disposable.add(it) }
        switchService.toggleAvailableObservable.subscribeIO { switchEnabledLiveData.postValue(it) }.let { disposable.add(it) }

        syncAmount(service.amount)
        syncCoin(service.coin)
        syncCoinAmount(fiatService.coinAmount)
        syncPrimaryInfo(fiatService.primaryInfo)
        syncSecondaryAmountInfo(fiatService.secondaryAmountInfo)
    }

    private fun syncAmount(amount: BigDecimal) {
        fiatService.setCoinAmount(amount)
    }

    private fun syncCoin(coin: Coin?) {
        val max = maxCoinDecimal
        coinDecimal = min(max, (coin?.decimal ?: max))

        fiatService.setCoin(coin)

        maxEnabledLiveData.postValue(isMaxSupported &&
                (service.balance ?: BigDecimal.ZERO) > BigDecimal.ZERO)
    }

    private fun syncCoinAmount(amount: BigDecimal) {
        service.onChangeAmount(amount)
    }

    private fun getPrefix(primaryInfo: FiatServiceSendEvm.PrimaryInfo): String? =
            if (primaryInfo is FiatServiceSendEvm.PrimaryInfo.Info) {
                primaryInfo.amountInfo?.let {
                    if (it is AmountInfo.CurrencyValueInfo) {
                        it.currencyValue.currency.symbol
                    } else null
                }
            } else null

    private fun getAmountString(primaryInfo: FiatServiceSendEvm.PrimaryInfo): String? =
            when (primaryInfo) {
                is FiatServiceSendEvm.PrimaryInfo.Info -> {
                    val amountInfo = primaryInfo.amountInfo
                    if (amountInfo == null || amountInfo.value <= BigDecimal.ZERO) {
                        null
                    } else {
                        val amount = amountInfo.value.setScale(min(amountInfo.decimal, maxCoinDecimal), RoundingMode.DOWN)
                        amount.stripTrailingZeros().toPlainString()
                    }
                }
                is FiatServiceSendEvm.PrimaryInfo.Amount -> {
                    val amount = primaryInfo.amount.setScale(maxCoinDecimal, RoundingMode.DOWN)
                    amount.stripTrailingZeros().toPlainString()
                }
            }

    private fun syncPrimaryInfo(primaryInfo: FiatServiceSendEvm.PrimaryInfo) {
        amountLiveData.postValue(getAmountString(primaryInfo))
        prefixLiveData.postValue(getPrefix(primaryInfo))
    }

    private fun syncSecondaryAmountInfo(amountInfo: AmountInfo?) {
        secondaryTextLiveData.postValue(amountInfo?.getFormatted())
    }

    fun areAmountsEqual(lhs: String?, rhs: String?): Boolean {
        val lhsDecimal = lhs?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val rhsDecimal = rhs?.toBigDecimalOrNull() ?: BigDecimal.ZERO

        return lhsDecimal.compareTo(rhsDecimal) == 0
    }

    fun onChangeAmount(amount: String?) {
        val amountDecimal = amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (amountDecimal != null && amountDecimal.scale() > validDecimal) {
            val amountNumber = amountDecimal.setScale(validDecimal, RoundingMode.FLOOR)
            val revertedInput = amountNumber.toPlainString()
            revertAmountLiveData.postValue(revertedInput)
        } else {
            fiatService.setAmount(amountDecimal)
        }
    }

    fun onClickMax() {
        service.balance?.let { balance ->
            fiatService.setCoinAmount(balance)
        }
    }

    fun onSwitch() {
        switchService.toggle()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    companion object {
        const val maxCoinDecimal = 8
    }

}
