package io.horizontalsystems.bankwallet.modules.send.evm.settings

import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.*
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext

class SendEvmSettingsService(
    private val feeService: IEvmFeeService,
    private val nonceService: SendEvmNonceService
) {
    var state: DataState<Transaction> = DataState.Loading
        private set(value) {
            field = value
            _stateFlow.update { value }
        }
    private val _stateFlow = MutableStateFlow(state)
    val stateFlow: Flow<DataState<Transaction>> = _stateFlow

    suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            feeService.transactionStatusObservable.asFlow().collect {
                sync()
            }
        }
        launch {
            nonceService.stateFlow.collect {
                sync()
            }
        }

        nonceService.start()
    }

    private fun sync() {
        val feeState = feeService.transactionStatus
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
