package io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

abstract class ISendTransactionService: ServiceState<SendTransactionServiceState>() {
    abstract fun start(coroutineScope: CoroutineScope)
    abstract fun setSendTransactionData(data: SendTransactionData)
    @Composable
    abstract fun GetContent(navController: NavController)
    abstract suspend fun sendTransaction()
    abstract val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
}

data class SendTransactionServiceState(
    val networkFee: SendModule.AmountData?
)
