package cash.p.terminal.modules.send.evm.settings

import cash.p.terminal.wallet.Warning
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.modules.evmfee.GasData
import cash.p.terminal.modules.evmfee.IEvmFeeService
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cash.p.terminal.modules.evmfee.Transaction as TransactionFeeData

class SendEvmSettingsService(
    private val feeService: IEvmFeeService,
    private val nonceService: SendEvmNonceService
) {
    private var feeState: DataState<TransactionFeeData>? = null

    var state: DataState<Transaction> = DataState.Loading
        private set(value) {
            field = value
            _stateFlow.tryEmit(value)
        }
    private val _stateFlow: MutableSharedFlow<DataState<Transaction>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stateFlow: Flow<DataState<Transaction>> = _stateFlow.asSharedFlow()

    suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            feeService.transactionStatusFlow.collect {
                feeState = it
                sync()
            }
        }
        launch {
            nonceService.stateFlow.collect {
                sync()
            }
        }
    }

    fun clear() {
        feeService.clear()
    }

    private fun sync() {
        val feeState = feeState
        val nonceState = nonceService.state

        state = when {
            feeState == DataState.Loading -> DataState.Loading
            nonceState == DataState.Loading -> DataState.Loading
            feeState is DataState.Error -> feeState
            nonceState is DataState.Error -> nonceState
            feeState is DataState.Success && nonceState is DataState.Success -> {
                val feeData = feeState.data
                val nonceData = nonceState.data

                val errors = feeData.errors.ifEmpty { nonceData.errors }
                val warnings = if (errors.isEmpty())
                    feeData.warnings.ifEmpty { nonceData.warnings }
                else
                    listOf()

                DataState.Success(
                    Transaction(
                        transactionData = feeData.transactionData,
                        gasData = feeData.gasData,
                        nonce = nonceData.nonce,
                        default = feeData.default && nonceData.default,
                        warnings = warnings,
                        errors = errors
                    )
                )
            }
            else -> DataState.Loading
        }
    }

    suspend fun reset() {
        feeService.reset()
        nonceService.reset()
    }

    data class Transaction(
        val transactionData: TransactionData,
        val gasData: GasData,
        val nonce: Long?,
        val default: Boolean,
        val warnings: List<Warning> = listOf(),
        val errors: List<Throwable> = listOf()
    )

}
