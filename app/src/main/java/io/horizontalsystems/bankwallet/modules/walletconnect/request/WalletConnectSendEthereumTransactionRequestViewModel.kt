package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectSendEthereumTransactionRequestService.State
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.reactivex.disposables.CompositeDisposable

class WalletConnectSendEthereumTransactionRequestViewModel(
        private val service: WalletConnectSendEthereumTransactionRequestService,
        private val coinService: CoinService,
        private val stringProvider: StringProvider
) : ViewModel() {

    val amountData: SendModule.AmountData
    val viewItems: List<WalletConnectRequestViewItem>
    val finishedLiveEvent = SingleLiveEvent<Boolean>()
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
        service.approve()
    }

    fun reject() {
        service.reject()
        finishedLiveEvent.postValue(false)
    }

    private fun sync(state: State) {
        if (state == State.Sent) {
            finishedLiveEvent.postValue(true)
            return
        }

        approveEnabledLiveData.postValue(state is State.Ready)
        rejectEnabledLiveData.postValue(state !is State.Sending)

        if (state is State.NotReady) {
            errorLiveData.postValue(convert(state.error))
        } else {
            errorLiveData.postValue(null)
        }
    }

    private fun convert(error: Throwable?) = when (val convertedError = error?.convertedError) {
        is WalletConnectSendEthereumTransactionRequestService.TransactionError.InsufficientBalance -> {
            val amountData = coinService.amountData(convertedError.requiredBalance)

            stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalance, amountData.getFormatted())
        }
        is EvmError.InsufficientBalanceWithFee,
        is EvmError.ExecutionReverted -> stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, coinService.coin.code)
        else -> convertedError?.message ?: convertedError?.javaClass?.simpleName
    }

}
