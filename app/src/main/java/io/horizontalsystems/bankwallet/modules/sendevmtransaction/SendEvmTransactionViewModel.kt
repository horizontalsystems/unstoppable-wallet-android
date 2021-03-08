package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.toHexString
import io.reactivex.disposables.CompositeDisposable

class SendEvmTransactionViewModel(
        private val service: SendEvmTransactionService,
        private val coinService: CoinService,
        private val stringProvider: StringProvider
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val sendEnabledLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String?>()

    val sendingLiveData = MutableLiveData<Boolean>()
    val sendSuccessLiveData = MutableLiveData<ByteArray>()
    val sendFailedLiveData = MutableLiveData<String>()

    val viewItems = listOf(
            ViewItem.To(service.toAddress.eip55),
            ViewItem.Amount(coinService.coinValue(service.amount).getFormatted()),
            ViewItem.Input(service.inputData.toHexString())
    )

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.sendStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
        sync(service.sendState)
    }

    fun send() {
        service.send()
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

    private fun sync(sendState: SendEvmTransactionService.SendState) =
            when (sendState) {
                SendEvmTransactionService.SendState.Idle -> Unit
                SendEvmTransactionService.SendState.Sending -> sendingLiveData.postValue(true)
                is SendEvmTransactionService.SendState.Sent -> sendSuccessLiveData.postValue(sendState.transactionHash)
                is SendEvmTransactionService.SendState.Failed -> sendFailedLiveData.postValue(convertError(sendState.error))
            }

    private fun convertError(error: Throwable) =
            when (val convertedError = error.convertedError) {
                is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                    stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalance, coinService.coinValue(convertedError.requiredBalance))
                }
                is EvmError.InsufficientBalanceWithFee,
                is EvmError.ExecutionReverted -> {
                    stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, coinService.coin.code)
                }
                else -> convertedError.message ?: convertedError.javaClass.simpleName
            }

    sealed class ViewItem(val value: String) {
        class To(value: String) : ViewItem(value)
        class Amount(value: String) : ViewItem(value)
        class Input(value: String) : ViewItem(value)
    }

}
