package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

abstract class ISendTransactionService: ServiceState<SendTransactionServiceState>() {
    abstract fun start(coroutineScope: CoroutineScope)
    abstract fun setSendTransactionData(data: SendTransactionData)
    @Composable
    abstract fun GetSettingsContent(navController: NavController)
    abstract suspend fun sendTransaction() : SendTransactionResult
    abstract val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
}

data class SendTransactionServiceState(
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendable: Boolean,
    val loading: Boolean,
    val fields: List<DataField>
)
