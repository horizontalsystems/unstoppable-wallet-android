package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.reactivex.disposables.CompositeDisposable

class WalletConnectSendEthereumTransactionRequestViewModel(
        private val service: WalletConnectSendEthereumTransactionRequestService,
        private val coinService: CoinService
) : ViewModel() {

    val amountData: SendModule.AmountData
    val viewItems: List<WalletConnectRequestViewItem>
    val approveLiveEvent = SingleLiveEvent<ByteArray>()
    val approveEnabledLiveData = MutableLiveData<Boolean>()
    val rejectEnabledLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String?>()

    private val disposable = CompositeDisposable()

    init {
        val transactionData = service.transactionData

        amountData = coinService.amountData(transactionData.value)

        val viewItems = mutableListOf<WalletConnectRequestViewItem>()

        viewItems.add(WalletConnectRequestViewItem.To(transactionData.to.eip55))
        if (transactionData.input.isNotEmpty()) {
            viewItems.add(WalletConnectRequestViewItem.Input(transactionData.input.toHexString()))
        }

        this.viewItems = viewItems

        service.stateObservable
                .subscribe {
                    sync(it)
                }
                .let {
                    disposable.add(it)
                }

        sync(service.state)
    }

    fun approve() {
        service.send()
    }

    private fun sync(state: WalletConnectSendEthereumTransactionRequestService.State) {
        if (state is WalletConnectSendEthereumTransactionRequestService.State.Sent) {
            approveLiveEvent.postValue(state.transactionHash)
            return
        }

        approveEnabledLiveData.postValue(state is WalletConnectSendEthereumTransactionRequestService.State.Ready)
        rejectEnabledLiveData.postValue(state !is WalletConnectSendEthereumTransactionRequestService.State.Sending)

        if (state is WalletConnectSendEthereumTransactionRequestService.State.NotReady) {
            errorLiveData.postValue(convert(state.error))
        } else {
            errorLiveData.postValue(null)
        }
    }

    private fun convert(error: Throwable?) = when (error) {
        is WalletConnectSendEthereumTransactionRequestService.TransactionError.InsufficientBalance -> {
            val amountData = coinService.amountData(error.requiredBalance)

            App.instance.getString(R.string.EthereumTransaction_Error_InsufficientBalance, amountData.getFormatted())
        }
        is JsonRpc.ResponseError.InsufficientBalance -> App.instance.getString(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, coinService.coin.code)
        is JsonRpc.ResponseError.RpcError -> error.error.message
        else -> error?.message ?: error?.javaClass?.simpleName
    }

}
