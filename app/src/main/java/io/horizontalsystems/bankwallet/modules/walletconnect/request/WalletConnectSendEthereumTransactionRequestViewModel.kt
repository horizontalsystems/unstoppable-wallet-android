package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EthereumCoinService
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.toHexString
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectSendEthereumTransactionRequestViewModel(
        private val service: WalletConnectSendEthereumTransactionRequestService,
        private val coinService: EthereumCoinService
) : ViewModel() {

    val amountData: SendModule.AmountData
    val viewItems: List<WalletConnectRequestViewItem>
    val approveLiveEvent = SingleLiveEvent<ByteArray>()
    val approveEnabledLiveData = MutableLiveData<Boolean>()
    val rejectEnabledLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<Throwable?>()

    private val disposable = CompositeDisposable()

    init {
        val transactionData = service.transactionData

        amountData = coinService.amountData(transactionData.value)

        viewItems = listOf(
                WalletConnectRequestViewItem.To(transactionData.to.eip55),
                WalletConnectRequestViewItem.Input(transactionData.input.toHexString())
        )

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
            SendError.InsufficientBalance(coinService.amountData(error.requiredBalance))
        }
        else -> error
    }


    sealed class SendError : Error() {
        class InsufficientBalance(val requiredBalance: SendModule.AmountData) : SendError()
    }

}
