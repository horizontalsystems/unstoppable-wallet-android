package io.horizontalsystems.bankwallet.modules.swap.confirmation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.net.UnknownHostException
import java.util.*

data class ConfirmationViewItem(
        val sendingTitle: String,
        val sendingValue: String,
        val receivingTitle: String,
        val receivingValue: String,
        val minMaxTitle: String,
        val minMaxValue: String,
        val price: String,
        val priceImpact: String,
        val swapFee: String,
        val transactionSpeed: String,
        val transactionFee: String
)

class ConfirmationPresenter(
        private val swapService: SwapModule.ISwapService,
        private val stringProvider: StringProvider,
        private val numberFormatter: IAppNumberFormatter
) : Clearable {

    private val disposables = CompositeDisposable()

    private val _swapButtonEnabled = MutableLiveData<Boolean>()
    val swapButtonEnabled: LiveData<Boolean> = _swapButtonEnabled

    private val _swapButtonTitle = MutableLiveData<String>()
    val swapButtonTitle: LiveData<String> = _swapButtonTitle

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        swapService.state
                .subscribeOn(Schedulers.io())
                .subscribe { swapState ->
                    _swapButtonEnabled.postValue(swapState == SwapState.SwapAllowed)

                    val swapButtonTitle = if (swapState == SwapState.Swapping) {
                        stringProvider.string(R.string.Swap_Swapping)
                    } else {
                        stringProvider.string(R.string.Swap)
                    }
                    _swapButtonTitle.postValue(swapButtonTitle)

                    if (swapState is SwapState.Failed) {
                        _error.postValue(errorText(swapState.error))
                    } else {
                        _error.postValue(null)
                    }
                }
                .let { disposables.add(it) }
    }

    fun onCancelConfirmation() {
        swapService.cancelProceed()
    }

    fun onSwap() {
        swapService.swap()
    }

    fun confirmationViewItem(): ConfirmationViewItem {
        val trade = swapService.trade?.dataOrNull

        val sendingInfo = sendingInfo()
        val receivingInfo = receivingInfo()
        val minMaxInfo = trade?.let { minMaxInfo(it) }

        val price = trade?.executionPrice?.let {
            "${trade.coinSending.code} = ${formatCoinAmount(it, trade.coinReceiving)}"
        }
        val priceImpact = trade?.priceImpact?.value?.toPlainString()?.let {
            stringProvider.string(R.string.Swap_Percent, it)
        }
        val swapFee = swapService.swapFee?.let {
            formatCoinAmount(it.value, it.coin)
        }
        val transactionSpeed = swapService.feeRatePriority.javaClass.simpleName.toLowerCase(Locale.ENGLISH).capitalize()
        val transactionFee = swapService.transactionFee?.let { formatFee(it) }

        return ConfirmationViewItem(
                sendingTitle = sendingInfo?.first ?: "",
                sendingValue = sendingInfo?.second ?: "",
                receivingTitle = receivingInfo?.first ?: "",
                receivingValue = receivingInfo?.second ?: "",
                minMaxTitle = minMaxInfo?.first ?: "",
                minMaxValue = minMaxInfo?.second ?: "",
                price = price ?: "",
                priceImpact = priceImpact ?: "",
                swapFee = swapFee ?: "",
                transactionSpeed = transactionSpeed,
                transactionFee = transactionFee ?: ""
        )
    }

    override fun clear() {
        disposables.dispose()
    }

    private fun sendingInfo(): Pair<String, String>? {
        val coinSending = swapService.coinSending
        val amountReceiving = swapService.amountSending ?: return null
        val title = stringProvider.string(R.string.Swap_Confirmation_Pay, coinSending.title)
        return Pair(title, formatCoinAmount(amountReceiving, coinSending))
    }

    private fun receivingInfo(): Pair<String, String>? {
        val coinReceiving = swapService.coinReceiving ?: return null
        val amountReceiving = swapService.amountReceiving ?: return null
        val title = stringProvider.string(R.string.Swap_Confirmation_Get, coinReceiving.title)
        val value = formatCoinAmount(amountReceiving, coinReceiving)
        return Pair(title, value)
    }

    private fun minMaxInfo(trade: Trade): Pair<String, String>? {
        var title: String? = null
        var value: String? = null
        when (trade.amountType) {
            AmountType.ExactSending -> {
                title = stringProvider.string(R.string.Swap_MinimumReceived)
                value = trade.minMaxAmount?.let { formatCoinAmount(it, trade.coinReceiving) }
            }
            AmountType.ExactReceiving -> {
                title = stringProvider.string(R.string.Swap_MaximumSold)
                value = trade.minMaxAmount?.let { formatCoinAmount(it, trade.coinSending) }
            }
        }
        return value?.let { Pair(title, it) }
    }

    private fun formatCoinAmount(amount: BigDecimal, coin: Coin): String {
        val maxFraction = if (coin.decimal < 8) coin.decimal else 8
        return numberFormatter.formatCoin(amount, coin.code, 0, maxFraction)
    }

    private fun formatFee(fee: Pair<CoinValue, CurrencyValue?>): String {
        val coinAmount = fee.first.let { formatCoinAmount(it.value, it.coin) }
        val fiatAmount = fee.second?.let {
            numberFormatter.formatFiat(it.value, it.currency.symbol, 2, 2)
        }
        return "$coinAmount${if (fiatAmount != null) " | $fiatAmount" else ""}"
    }

    private fun errorText(swapError: SwapModule.SwapError): String {
        return if (swapError is SwapModule.SwapError.Other) {
            if (swapError.error.cause is UnknownHostException) {
                stringProvider.string(R.string.Hud_Text_NoInternet)
            } else {
                swapError.error.message ?: swapError.error.javaClass.simpleName
            }
        } else {
            swapError.javaClass.simpleName
        }
    }

}
