package io.horizontalsystems.bankwallet.modules.swap.confirmation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class SwapConfirmationViewModel(
        private val service: SwapService,
        private val tradeService: SwapTradeService,
        private val transactionService: EvmTransactionService,
        private val coinService: CoinService,
        private val numberFormatter: IAppNumberFormatter,
        private val formatter: SwapViewItemHelper,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val loadingLiveData = SingleLiveEvent<Unit>()
    private val completedLiveData = SingleLiveEvent<Unit>()
    private val errorLiveEvent = SingleLiveEvent<String?>()
    private val amountLiveData = MutableLiveData<SwapModule.ConfirmationAmountViewItem>()
    private val additionalLiveData = MutableLiveData<List<SwapModule.ConfirmationAdditionalViewItem>>()

    fun loadingLiveData(): LiveData<Unit> = loadingLiveData
    fun completedLiveData(): LiveData<Unit> = completedLiveData
    fun errorLiveEvent(): SingleLiveEvent<String?> = errorLiveEvent
    fun amountLiveData(): LiveData<SwapModule.ConfirmationAmountViewItem> = amountLiveData
    fun additionalLiveData(): LiveData<List<SwapModule.ConfirmationAdditionalViewItem>> = additionalLiveData

    init {
        buildViewItems()
        subscribeOnService()
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun swap() {
        service.swap()
    }

    private fun subscribeOnService() {
        service.swapEventObservable
                .subscribe {
                    sync(it)
                }
                .let { disposables.add(it) }
    }

    private fun buildViewItems() {
        val coinIn = tradeService.coinFrom ?: return
        val amountIn = tradeService.amountFrom ?: return
        val amountOut = tradeService.amountTo ?: return
        val coinOut = tradeService.coinTo ?: return

        val payValue = numberFormatter.formatCoin(amountIn, coinIn.code, 0, 8)
        val getValue = numberFormatter.formatCoin(amountOut, coinOut.code, 0, 8)

        val amountData = SwapModule.ConfirmationAmountViewItem(coinIn.title, payValue, coinOut.title, getValue)
        amountLiveData.postValue(amountData)

        val additionalData = mutableListOf<SwapModule.ConfirmationAdditionalViewItem>()

        formatter.slippage(tradeService.tradeOptions.allowedSlippage)?.let {
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.SwapSettings_SlippageTitle), it))
        }

        formatter.deadline(tradeService.tradeOptions.ttl)?.let {
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.SwapSettings_DeadlineTitle), it))
        }

        tradeService.tradeOptions.recipient?.hex?.let { recipient ->
            var address = recipient
            tradeService.tradeRecipientDomain?.let {
                address = "$it ($address)"
            }

            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.SwapSettings_RecipientAddressTitle), address))
        }

        val trade = (tradeService.state as? SwapTradeService.State.Ready)?.trade ?: return

        formatter.guaranteedAmountViewItem(trade.tradeData, coinIn, coinOut)?.let { viewItem ->
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(viewItem.title, viewItem.value))
        }

        formatter.price(trade.tradeData.executionPrice, coinIn, coinOut)?.let {
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.Swap_Price), it))
        }

        formatter.priceImpactViewItem(trade)?.let {
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.Swap_PriceImpact), it.value))
        }

        transactionService.transactionStatus.dataOrNull?.let { transaction ->
            val estimatedFee = coinService.amountData(transaction.gasData.estimatedFee).getFormatted()
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.Swap_EstimatedFee), estimatedFee))

            val maxFee = coinService.amountData(transaction.gasData.fee).getFormatted()
            additionalData.add(SwapModule.ConfirmationAdditionalViewItem(stringProvider.string(R.string.Swap_MaxFee), maxFee))
        }

        additionalLiveData.postValue(additionalData)
    }

    private fun sync(event: SwapService.SwapEvent) {
        when (event) {
            SwapService.SwapEvent.Swapping -> loadingLiveData.postValue(Unit)
            SwapService.SwapEvent.Completed -> completedLiveData.postValue(Unit)
            is SwapService.SwapEvent.Failed -> errorLiveEvent.postValue(event.error.message)
        }
    }

}
