package cash.p.terminal.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.core.ServiceState
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.modules.send.SendModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

abstract class ISendTransactionService: ServiceState<SendTransactionServiceState>() {
    abstract fun start(coroutineScope: CoroutineScope)
    abstract fun setSendTransactionData(data: SendTransactionData)
    @Composable
    abstract fun GetSettingsContent(navController: NavController)
    abstract suspend fun sendTransaction()
    abstract val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
}

data class SendTransactionServiceState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendable: Boolean,
    val loading: Boolean,
    val uuid: String = UUID.randomUUID().toString()
)
