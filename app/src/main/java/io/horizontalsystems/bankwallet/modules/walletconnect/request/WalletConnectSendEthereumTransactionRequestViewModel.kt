package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EthereumCoinService
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectSendEthereumTransactionRequestViewModel(private val service: EthereumCoinService, private val request: WalletConnectSendEthereumTransactionRequest) : ViewModel() {

    val resultLiveData = MutableLiveData<Unit>()
    val amountViewItemLiveData = MutableLiveData<WalletConnectRequestAmountViewItem>()
    val viewItemsLiveData = MutableLiveData<List<WalletConnectRequestViewItem>>()

    init {
        syncTransaction(request.transaction)
    }

    private fun syncTransaction(transaction: WalletConnectTransaction) {
        val value = convert(transaction.value, service.ethereumCoin)

        val amountData = service.amountData(transaction.value)

        val amountViewItem = WalletConnectRequestAmountViewItem(amountData.primary, amountData.secondary)
        amountViewItemLiveData.postValue(amountViewItem)

        val viewItems = listOf(
                WalletConnectRequestViewItem.From(transaction.from.eip55),
                WalletConnectRequestViewItem.To(transaction.to.eip55)
        )
        viewItemsLiveData.postValue(viewItems)
    }

    private fun convert(value: BigInteger, coin: Coin): BigDecimal {
        return BigDecimal(value, coin.decimal)
    }

    fun approve() {
        resultLiveData.postValue(Unit)
    }

}
