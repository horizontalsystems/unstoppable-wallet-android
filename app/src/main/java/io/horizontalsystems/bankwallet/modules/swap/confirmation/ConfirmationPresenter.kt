package io.horizontalsystems.bankwallet.modules.swap.confirmation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
        val transactionFee: String,
        val slippage: String?,
        val deadline: String?,
        val recipientAddress: String?
)

class ConfirmationPresenter(
        private val swapService: SwapModule.ISwapService,
        private val stringProvider: StringProvider,
        private val formatter: SwapItemFormatter,
        private val ethereumCoinService: CoinService
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

    fun confirmationViewItem(): ConfirmationViewItem? {
        val trade = swapService.trade?.dataOrNull ?: return null
        val coinSending = swapService.coinSending ?: return null
        val amountSending = swapService.amountSending ?: return null
        val coinReceiving = swapService.coinReceiving ?: return null
        val amountReceiving = swapService.amountReceiving ?: return null
        val swapFee = swapService.swapFee ?: return null
        val transactionFee = swapService.transactionFee ?: return null
        val minMaxValue = trade.minMaxAmount ?: return null
        val executionPrice = trade.executionPrice ?: return null
        val priceImpact = trade.priceImpact ?: return null

        val slippage = if (swapService.currentSwapSettings.slippage == swapService.defaultSwapSettings.slippage)
            null
        else
            stringProvider.string(R.string.Swap_Percent, swapService.currentSwapSettings.slippage.toString())

        val txDeadline = if (swapService.currentSwapSettings.deadline == swapService.defaultSwapSettings.deadline)
            null
        else
            stringProvider.string(R.string.Duration_Minutes, swapService.currentSwapSettings.deadline)

        val recipientAddress = swapService.currentSwapSettings.recipientAddress

        return ConfirmationViewItem(
                sendingTitle = stringProvider.string(R.string.Swap_Confirmation_Pay, coinSending.title),
                sendingValue = formatter.coinAmount(amountSending, coinSending),
                receivingTitle = stringProvider.string(R.string.Swap_Confirmation_Get, coinReceiving.title),
                receivingValue = formatter.coinAmount(amountReceiving, coinReceiving),
                minMaxTitle = formatter.minMaxTitle(trade.amountType),
                minMaxValue = formatter.minMaxValue(minMaxValue, coinSending, coinReceiving, trade.amountType),
                price = formatter.executionPrice(executionPrice, trade.coinSending, trade.coinReceiving),
                priceImpact = formatter.priceImpact(priceImpact),
                swapFee = formatter.coinAmount(swapFee.value, swapFee.coin),
                transactionSpeed = swapService.gasPriceType.javaClass.simpleName.toLowerCase(Locale.ENGLISH).capitalize(),
                transactionFee =  ethereumCoinService.amountData(transactionFee).getFormatted(),
                slippage = slippage,
                deadline = txDeadline,
                recipientAddress = recipientAddress
        )
    }

    override fun clear() {
        disposables.dispose()
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
