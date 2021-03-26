package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoAddressMapper
import io.horizontalsystems.ethereumkit.core.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.disposables.CompositeDisposable

class SendEvmTransactionViewModel(
        private val service: SendEvmTransactionService,
        private val coinServiceFactory: EvmCoinServiceFactory,
        private val stringProvider: StringProvider
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val sendEnabledLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String?>()

    val sendingLiveData = MutableLiveData<Unit>()
    val sendSuccessLiveData = MutableLiveData<ByteArray>()
    val sendFailedLiveData = MutableLiveData<String>()

    val viewItemsLiveData = MutableLiveData<List<SectionViewItem>>()

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.txDataStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.sendStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
        sync(service.txDataState)
        sync(service.sendState)
    }

    fun send(logger: AppLogger) {
        service.send(logger)
    }

    private fun sync(state: SendEvmTransactionService.State) =
            when (state) {
                SendEvmTransactionService.State.Ready -> {
                    sendEnabledLiveData.postValue(true)
                    errorLiveData.postValue(null)
                }
                is SendEvmTransactionService.State.NotReady -> {
                    sendEnabledLiveData.postValue(false)
                    errorLiveData.postValue(state.errors.firstOrNull()?.let { convertError(it) })
                }
            }

    private fun sync(txDataState: SendEvmTransactionService.TxDataState) {
        val viewItems = txDataState.decoration?.let {
            getViewItems(it, txDataState.additionalInfo)
        } ?: getFallbackViewItems(txDataState.transactionData)

        viewItemsLiveData.postValue(viewItems)
    }

    private fun sync(sendState: SendEvmTransactionService.SendState) =
            when (sendState) {
                SendEvmTransactionService.SendState.Idle -> Unit
                SendEvmTransactionService.SendState.Sending -> {
                    sendEnabledLiveData.postValue(false)
                    sendingLiveData.postValue(Unit)
                }
                is SendEvmTransactionService.SendState.Sent -> sendSuccessLiveData.postValue(sendState.transactionHash)
                is SendEvmTransactionService.SendState.Failed -> sendFailedLiveData.postValue(convertError(sendState.error))
            }

    private fun getViewItems(decoration: TransactionDecoration, additionalInfo: SendEvmData.AdditionalInfo?): List<SectionViewItem>? =
            when (decoration) {
                is TransactionDecoration.Transfer -> getTransferViewItems(decoration, additionalInfo)
                is TransactionDecoration.Eip20Transfer -> getEip20TransferViewItems(decoration, additionalInfo)
                is TransactionDecoration.Eip20Approve -> getEip20ApproveViewItems(decoration)
                is TransactionDecoration.Swap -> getSwapViewItems(decoration, additionalInfo)
                else -> null
            }

    private fun getTransferViewItems(transfer: TransactionDecoration.Transfer, additionalInfo: SendEvmData.AdditionalInfo?): List<SectionViewItem> {
        val viewItems = mutableListOf(
                ViewItem.Subhead(
                        stringProvider.string(R.string.Send_Confirmation_YouSend),
                        coinServiceFactory.baseCoinService.coin.title
                ),
                ViewItem.Value(
                        stringProvider.string(R.string.Send_Confirmation_Amount),
                        coinServiceFactory.baseCoinService.amountData(transfer.value).getFormatted(), ValueType.Outgoing
                )
        )
        val addressValue = transfer.to.eip55
        val addressTitle = additionalInfo?.sendInfo?.domain ?: TransactionInfoAddressMapper.map(addressValue)
        viewItems.add(ViewItem.Address(
                stringProvider.string(R.string.Send_Confirmation_To),
                addressTitle,
                value = addressValue)
        )
        return listOf(SectionViewItem(viewItems))
    }

    private fun getEip20TransferViewItems(eip20Transfer: TransactionDecoration.Eip20Transfer, additionalInfo: SendEvmData.AdditionalInfo?): List<SectionViewItem>? =
            coinServiceFactory.getCoinService(eip20Transfer.contractAddress)?.let { coinService ->
                val viewItems = mutableListOf(
                        ViewItem.Subhead(stringProvider.string(R.string.Send_Confirmation_YouSend), coinService.coin.title),
                        ViewItem.Value(stringProvider.string(R.string.Send_Confirmation_Amount), coinService.amountData(eip20Transfer.value).getFormatted(), ValueType.Outgoing)
                )
                val addressValue = eip20Transfer.to.eip55
                val addressTitle = additionalInfo?.sendInfo?.domain
                        ?: TransactionInfoAddressMapper.map(addressValue)
                viewItems.add(
                        ViewItem.Address(stringProvider.string(R.string.Send_Confirmation_To), addressTitle, value = addressValue)
                )
                listOf(SectionViewItem(viewItems))
            }

    private fun getEip20ApproveViewItems(eip20Approve: TransactionDecoration.Eip20Approve): List<SectionViewItem>? =
            coinServiceFactory.getCoinService(eip20Approve.contractAddress)?.let { coinService ->
                val addressValue = eip20Approve.spender.eip55
                val addressTitle = TransactionInfoAddressMapper.map(addressValue)
                val viewItems = listOf(
                        ViewItem.Subhead(stringProvider.string(R.string.Approve_YouApprove), coinService.coin.title),
                        ViewItem.Value(stringProvider.string(R.string.Send_Confirmation_Amount), coinService.amountData(eip20Approve.value).getFormatted(), ValueType.Regular),
                        ViewItem.Address(stringProvider.string(R.string.Approve_Spender), addressTitle, addressValue)
                )
                listOf(SectionViewItem(viewItems))
            }

    private fun getSwapViewItems(swap: TransactionDecoration.Swap, additionalInfo: SendEvmData.AdditionalInfo?): List<SectionViewItem>? {
        val coinServiceIn = getCoinService(swap.tokenIn) ?: return null
        val coinServiceOut = getCoinService(swap.tokenOut) ?: return null

        val info = additionalInfo?.swapInfo
        val sections = mutableListOf<SectionViewItem>()

        when (val trade = swap.trade) {
            is TransactionDecoration.Swap.Trade.ExactIn -> {
                sections.add(SectionViewItem(listOf(
                        ViewItem.Subhead(stringProvider.string(R.string.Swap_FromAmountTitle), coinServiceIn.coin.title),
                        ViewItem.Value(stringProvider.string(R.string.Send_Confirmation_Amount), coinServiceIn.amountData(trade.amountIn).getFormatted(), ValueType.Outgoing)
                )))
                sections.add(SectionViewItem(listOf(
                        ViewItem.Subhead(stringProvider.string(R.string.Swap_ToAmountTitle), coinServiceOut.coin.title),
                        getEstimatedSwapAmount(info?.let { coinServiceOut.amountData(it.estimatedOut).getFormatted() }, ValueType.Incoming),
                        ViewItem.Value(stringProvider.string(R.string.Swap_Confirmation_Guaranteed), coinServiceOut.amountData(trade.amountOutMin).getFormatted(), ValueType.Regular)
                )))
            }
            is TransactionDecoration.Swap.Trade.ExactOut -> {
                sections.add(SectionViewItem(listOf(
                        ViewItem.Subhead(stringProvider.string(R.string.Swap_FromAmountTitle), coinServiceIn.coin.title),
                        getEstimatedSwapAmount(info?.let { coinServiceOut.amountData(it.estimatedOut).getFormatted() }, ValueType.Outgoing),
                        ViewItem.Value(stringProvider.string(R.string.Swap_Confirmation_Maximum), coinServiceIn.amountData(trade.amountInMax).getFormatted(), ValueType.Regular)
                )))
                sections.add(SectionViewItem(listOf(
                        ViewItem.Subhead(stringProvider.string(R.string.Swap_ToAmountTitle), coinServiceOut.coin.title),
                        ViewItem.Value(stringProvider.string(R.string.Swap_Confirmation_Guaranteed), coinServiceOut.amountData(trade.amountOut).getFormatted(), ValueType.Regular)
                )))
            }
        }

        val otherViewItems = mutableListOf<ViewItem>()
        info?.slippage?.let {
            otherViewItems.add(ViewItem.Value(stringProvider.string(R.string.SwapSettings_SlippageTitle), it, ValueType.Regular))
        }
        info?.deadline?.let {
            otherViewItems.add(ViewItem.Value(stringProvider.string(R.string.SwapSettings_DeadlineTitle), it, ValueType.Regular))
        }
        if (swap.to != service.ownAddress) {
            val addressValue = swap.to.eip55
            val addressTitle = info?.recipientDomain
                    ?: TransactionInfoAddressMapper.map(addressValue)
            otherViewItems.add(ViewItem.Address(stringProvider.string(R.string.SwapSettings_RecipientAddressTitle), addressTitle, addressValue))
        }
        info?.price?.let {
            otherViewItems.add(ViewItem.Value(stringProvider.string(R.string.Swap_Price), it, ValueType.Regular))
        }
        info?.priceImpact?.let {
            otherViewItems.add(ViewItem.Value(stringProvider.string(R.string.Swap_PriceImpact), it, ValueType.Regular))
        }
        if (otherViewItems.isNotEmpty()) {
            sections.add(SectionViewItem(otherViewItems))
        }

        return sections
    }

    private fun getEstimatedSwapAmount(value: String?, type: ValueType): ViewItem {
        val title = stringProvider.string(R.string.Swap_Confirmation_Estimated)
        return value?.let { ViewItem.Value(title, it, type) }
                ?: ViewItem.Value(title, stringProvider.string(R.string.NotAvailable), ValueType.Disabled)
    }

    private fun getCoinService(token: TransactionDecoration.Swap.Token) = when (token) {
        TransactionDecoration.Swap.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is TransactionDecoration.Swap.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getFallbackViewItems(transactionData: TransactionData): List<SectionViewItem> {
        val addressValue = transactionData.to.eip55
        val viewItems = listOf(
                ViewItem.Value(stringProvider.string(R.string.Send_Confirmation_Amount), coinServiceFactory.baseCoinService.amountData(transactionData.value).getFormatted(), ValueType.Outgoing),
                ViewItem.Address(stringProvider.string(R.string.Send_Confirmation_To), addressValue, addressValue),
                ViewItem.Input(transactionData.input.toHexString())
        )
        return listOf(SectionViewItem(viewItems))
    }

    private fun convertError(error: Throwable) =
            when (val convertedError = error.convertedError) {
                is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                    stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalance, coinServiceFactory.baseCoinService.coinValue(convertedError.requiredBalance).getFormatted())
                }
                is EvmError.InsufficientBalanceWithFee,
                is EvmError.ExecutionReverted -> {
                    stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, coinServiceFactory.baseCoinService.coin.code)
                }
                else -> convertedError.message ?: convertedError.javaClass.simpleName
            }
}

data class SectionViewItem(
        val viewItems: List<ViewItem>
)

sealed class ViewItem {
    class Subhead(val title: String, val value: String) : ViewItem()
    class Value(val title: String, val value: String, val type: ValueType) : ViewItem()
    class Address(val title: String, val valueTitle: String, val value: String) : ViewItem()
    class Input(val value: String) : ViewItem()
}

enum class ValueType {
    Regular, Disabled, Outgoing, Incoming
}
