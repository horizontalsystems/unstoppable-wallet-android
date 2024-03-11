package cash.p.terminal.modules.swapxxx.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

interface ISendTransactionService {
    @Composable
    fun GetContent(navController: NavController)
    fun setSendTransactionData(data: SendTransactionData)
}
