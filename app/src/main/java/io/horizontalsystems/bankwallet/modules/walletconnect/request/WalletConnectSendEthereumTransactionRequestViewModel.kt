package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectSendEthereumTransactionRequestViewModel(private val service: WalletConnectSendEthereumTransactionRequestService, private val request: WalletConnectSendEthereumTransactionRequest) : ViewModel() {

    val resultLiveData = MutableLiveData<Unit>()
    val amountViewItemLiveData = MutableLiveData<WalletConnectRequestAmountViewItem>()
    val viewItemsLiveData = MutableLiveData<List<WalletConnectRequestViewItem>>()

    init {
        syncTransaction(request.transaction)
    }

    private fun syncTransaction(transaction: WalletConnectTransaction) {
        val value = convert(transaction.value, service.ethereumCoin)

        val primaryAmountInfo: SendModule.AmountInfo
        val secondaryAmountInfo: SendModule.AmountInfo?

        val coinValue = CoinValue(service.ethereumCoin, value)
        val rate = service.ethereumRate
        if (rate != null) {
            primaryAmountInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(rate.currency, rate.value * value))
            secondaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        } else {
            primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            secondaryAmountInfo = null
        }

        val amountViewItem = WalletConnectRequestAmountViewItem(primaryAmountInfo, secondaryAmountInfo)
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
