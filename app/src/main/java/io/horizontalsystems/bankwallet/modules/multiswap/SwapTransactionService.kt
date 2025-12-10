package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceState
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwapTransactionService : ServiceState<SwapTransactionService.State>() {
    private var sendTransactionService: AbstractSendTransactionService? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var sendTransactionSettings: SendTransactionSettings? = null
    private var mevProtectionEnabled = false
    private var loading = true
    private var sendTransactionState: SendTransactionServiceState? = null
    private var mevProtectionAvailable = false
    private var hasSettings = false

    suspend fun setSendTransactionData(sendTransactionData: SendTransactionData?) {
        sendTransactionData?.let { sendTransactionService?.setSendTransactionData(it) }
    }

    fun setToken(token: Token?) {
        sendTransactionService = token?.let { SendTransactionServiceFactory.create(it) }

        sendTransactionState = sendTransactionService?.stateFlow?.value
        mevProtectionAvailable = sendTransactionService?.mevProtectionAvailable ?: false
        hasSettings = sendTransactionService?.hasSettings ?: false

        emitState()
    }

    fun start() {
        coroutineScope.coroutineContext.cancelChildren()

        coroutineScope.launch {
            sendTransactionService?.sendTransactionSettingsFlow?.collect {
                sendTransactionSettings = it

                emitState()
            }
        }

        coroutineScope.launch {
            sendTransactionService?.stateFlow?.collect { transactionState ->
                sendTransactionState = transactionState

                loading = transactionState.loading

                emitState()
            }
        }

        sendTransactionService?.start(coroutineScope)
    }

    fun stop() {
        coroutineScope.coroutineContext.cancelChildren()
    }

    suspend fun swap() = withContext(Dispatchers.Default) {
//        stat(page = StatPage.SwapConfirmation, event = StatEvent.Send)

        sendTransactionService?.sendTransaction(mevProtectionEnabled)
    }

    fun toggleMevProtection(enabled: Boolean) {
        mevProtectionEnabled = enabled

        emitState()
    }

    override fun createState() = State(
        sendTransactionSettings = sendTransactionSettings,
        mevProtectionEnabled = mevProtectionEnabled,
        loading = loading,
        sendTransactionState = sendTransactionState,
        mevProtectionAvailable = mevProtectionAvailable,
        hasSettings = hasSettings
    )

    data class State(
        val sendTransactionSettings: SendTransactionSettings?,
        val mevProtectionEnabled: Boolean,
        val loading: Boolean,
        val sendTransactionState: SendTransactionServiceState?,
        val mevProtectionAvailable: Boolean,
        val hasSettings: Boolean
    )

}
