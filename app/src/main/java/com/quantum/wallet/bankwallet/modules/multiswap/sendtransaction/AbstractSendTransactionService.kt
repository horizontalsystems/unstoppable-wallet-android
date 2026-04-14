package com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.core.ethereum.CautionViewItem
import com.quantum.wallet.bankwallet.entities.CoinValue
import com.quantum.wallet.bankwallet.entities.CurrencyValue
import com.quantum.wallet.bankwallet.modules.multiswap.ui.DataField
import com.quantum.wallet.bankwallet.modules.send.SendModule
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

abstract class AbstractSendTransactionService(val hasSettings: Boolean, val hasNonceSettings: Boolean): ServiceState<SendTransactionServiceState>() {
    open val supportsMevProtection: Boolean = false
    abstract val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
    protected var uuid = UUID.randomUUID().toString()

    private val baseCurrency = App.currencyManager.baseCurrency

    abstract fun start(coroutineScope: CoroutineScope)
    abstract suspend fun setSendTransactionData(data: SendTransactionData)
    @Composable
    open fun GetSettingsContent(navController: NavController) = Unit
    @Composable
    open fun GetNonceSettingsContent(navController: NavController) = Unit
    abstract suspend fun sendTransaction(mevProtectionEnabled: Boolean = false): SendTransactionResult

    fun refreshUuid() {
        uuid = UUID.randomUUID().toString()
    }

    protected fun getAmountData(coinValue: CoinValue): SendModule.AmountData {
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)

        val secondaryAmountInfo = getRate(coinValue.coin)?.let { rate ->
            SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(rate.currency, rate.value * coinValue.value))
        }
        return SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

    private fun getRate(coin: Coin): CurrencyValue? {
        return App.marketKit.coinPrice(coin.uid, baseCurrency.code)?.let {
            CurrencyValue(baseCurrency, it.value)
        }
    }
}

data class SendTransactionServiceState(
    val uuid: String,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendable: Boolean,
    val loading: Boolean,
    val fields: List<DataField>,
)
