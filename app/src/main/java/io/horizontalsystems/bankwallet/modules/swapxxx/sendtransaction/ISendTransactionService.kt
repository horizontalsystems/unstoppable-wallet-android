package cash.p.terminal.modules.swapxxx.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.core.ServiceState
import kotlinx.coroutines.CoroutineScope

abstract class ISendTransactionService: ServiceState<SendTransactionSettings>() {
    abstract fun start(coroutineScope: CoroutineScope)
    abstract fun setSendTransactionData(data: SendTransactionData)
    @Composable
    abstract fun GetContent(navController: NavController)
    abstract suspend fun sendTransaction()
}
