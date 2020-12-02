package io.horizontalsystems.bankwallet.modules.swap_new.coincard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

class SwapCoinCardViewModel(
        private val coinCardService: ISwapCoinCardService,
        private val formatter: SwapItemFormatter,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var validDecimals = maxValidDecimals

    private val amountLiveData = MutableLiveData<String?>()
    private val revertAmountLiveData = MutableLiveData<String>()
    private val balanceLiveData = MutableLiveData<String?>()
    private val balanceErrorLiveData = MutableLiveData<Boolean>()
    private val tokenCodeLiveData = MutableLiveData<String?>()
    private val isEstimatedLiveData = MutableLiveData<Boolean>()

    //region outputs
    fun amountLiveData(): LiveData<String?> = amountLiveData
    fun revertAmountLiveData(): LiveData<String> = revertAmountLiveData
    fun balanceLiveData(): LiveData<String?> = balanceLiveData
    fun balanceErrorLiveData(): LiveData<Boolean> = balanceErrorLiveData
    fun tokenCodeLiveData(): LiveData<String?> = tokenCodeLiveData
    fun isEstimatedLiveData(): LiveData<Boolean> = isEstimatedLiveData

    val tokensForSelection: List<SwapModule.CoinBalanceItem>
        get() = coinCardService.tokensForSelection

    fun onSelectCoin(coin: Coin) {
        coinCardService.onSelectCoin(coin)
    }

    fun onChangeAmount(amount: String?) {
        coinCardService.onChangeAmount(validateAmount(amount))
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
    }

    private fun syncEstimated(estimated: Boolean) {
        isEstimatedLiveData.postValue(estimated)
    }

    private fun syncAmount(amount: BigDecimal?) {
        val amountString = amount?.setScale(validDecimals, RoundingMode.FLOOR)?.toPlainString()
        amountLiveData.postValue(amountString)
    }

    private fun syncCoin(coin: Coin?) {
        validDecimals = min(maxValidDecimals, coin?.decimal ?: maxValidDecimals)
        tokenCodeLiveData.postValue(coin?.code)
    }

    private fun syncBalance(balance: BigDecimal?) {
        val coin = coinCardService.coin
        val formattedBalance = when {
            coin == null -> stringProvider.string(R.string.NotAvailable)
            balance == null -> null
            else -> formatter.coinAmount(balance, coin, validDecimals)
        }
        balanceLiveData.postValue(formattedBalance)
    }

    private fun syncError(error: Throwable?) {
        balanceErrorLiveData.postValue(error != null)
    }

    private fun validateAmount(amount: String?): BigDecimal? {
        val newAmount = amount?.toBigDecimalOrNull()
        return when {
            newAmount == null -> null
            newAmount.scale() > validDecimals -> {
                val amountWithValidScale = newAmount.setScale(validDecimals, RoundingMode.FLOOR)
                revertAmountLiveData.postValue(amountWithValidScale.toPlainString())
                amountWithValidScale
            }
            else -> newAmount
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    companion object {
        private const val maxValidDecimals = 8
    }

}
