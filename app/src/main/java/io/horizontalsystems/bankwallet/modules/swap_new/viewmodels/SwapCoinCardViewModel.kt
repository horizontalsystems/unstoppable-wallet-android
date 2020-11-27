package io.horizontalsystems.bankwallet.modules.swap_new.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService
import io.horizontalsystems.bankwallet.modules.swap_new.SwapTradeService
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min


abstract class SwapCoinCardViewModel(
        protected val service: SwapService,
        protected val tradeService: SwapTradeService,
        protected val formatter: SwapItemFormatter,
        protected val stringProvider: StringProvider
) : ViewModel() {

    //region outputs
    abstract val title: String
    abstract val tokensForSelection: List<CoinBalanceItem>

    fun amountLiveData(): LiveData<String?> = amountLiveData
    fun revertAmountLiveData(): LiveData<String> = revertAmountLiveData
    fun balanceLiveData(): LiveData<String?> = balanceLiveData
    fun balanceErrorLiveData(): LiveData<Boolean> = balanceErrorLiveData
    fun tokenCodeLiveData(): LiveData<String?> = tokenCodeLiveData
    fun isEstimatedLiveData(): LiveData<Boolean> = isEstimatedLiveData

    abstract fun onSelectCoin(coinBalanceItem: CoinBalanceItem)
    abstract fun onChangeAmount(amount: String?)
    //endregion

    protected val disposables = CompositeDisposable()

    protected abstract val tradeType: TradeType
    protected abstract val coin: Coin?

    protected val amountLiveData = MutableLiveData<String?>()
    protected val revertAmountLiveData = MutableLiveData<String>()
    protected val balanceLiveData = MutableLiveData<String?>()
    protected val balanceErrorLiveData = MutableLiveData<Boolean>()
    protected val tokenCodeLiveData = MutableLiveData<String?>()
    protected val isEstimatedLiveData = MutableLiveData<Boolean>()

    private var validDecimals = maxValidDecimals

    protected open fun subscribeToServices() {
        onUpdateAmountType(tradeService.tradeType)
        tradeService.tradeTypeObservable
                .subscribeOn(Schedulers.io())
                .subscribe { onUpdateAmountType(it) }
                .let { disposables.add(it) }

        service.errorsObservable
                .subscribeOn(Schedulers.io())
                .subscribe { handleErrors(it) }
                .let { disposables.add(it) }
    }

    private fun onUpdateAmountType(type: TradeType) {
        isEstimatedLiveData.postValue(tradeType != type)
    }

    protected open fun handleErrors(errors: List<Throwable>) {}

    protected fun onUpdateCoin(coin: Coin?) {
        validDecimals = min(maxValidDecimals, coin?.decimal ?: maxValidDecimals)
        tokenCodeLiveData.postValue(coin?.code)
    }

    protected fun onUpdateBalance(balance: BigDecimal?) {
        val coin = coin
        val formattedBalance = when {
            coin == null -> stringProvider.string(R.string.NotAvailable)
            balance == null -> null
            else -> formatter.coinAmount(balance, coin, validDecimals)
        }
        balanceLiveData.postValue(formattedBalance)
    }

    protected fun update(amount: BigDecimal?) {
        amountLiveData.postValue(amount?.setScale(validDecimals, RoundingMode.FLOOR)?.toPlainString())
    }

    protected fun validateAmount(amount: String?): BigDecimal? {
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
