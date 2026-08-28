package io.horizontalsystems.walletkit.modules.evmfee

import io.horizontalsystems.walletkit.core.EvmError
import io.horizontalsystems.walletkit.core.convertedError
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.math.BigInteger

class EvmFeeService(
    private val evmKit: EthereumKit,
    private val gasPriceService: IEvmGasPriceService,
    private val gasDataService: EvmCommonGasDataService,
    private var transactionData: TransactionData? = null,
) : IEvmFeeService {

    private var gasLimit: Long? = null
    private var gasPriceInfoState: DataState<GasPriceInfo> = DataState.Loading
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var gasPriceInfoJob: Job? = null

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    // Null until the account state lands. A missing balance is not a zero balance, so a check
    // that would reject the transfer has to read it through this and stand down while it is null.
    private val knownEvmBalance: BigInteger?
        get() = evmKit.accountState?.balance

    private val _transactionStatusFlow: MutableSharedFlow<DataState<Transaction>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val transactionStatusFlow = _transactionStatusFlow.asSharedFlow()

    fun start() {
        coroutineScope.launch {
            gasPriceService.stateFlow.collect {
                gasPriceInfoState = it
                sync()
            }
        }
    }

    override fun reset() {
        gasPriceService.setRecommended()
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    private fun sync() {
        when (val gasPriceInfoState = gasPriceInfoState) {
            is DataState.Error -> {
                _transactionStatusFlow.tryEmit(gasPriceInfoState)
            }
            DataState.Loading -> {
                _transactionStatusFlow.tryEmit(DataState.Loading)
            }
            is DataState.Success -> {
                sync(gasPriceInfoState.data)
            }
        }
    }

    private fun sync(gasPriceInfo: GasPriceInfo) {
        gasPriceInfoJob?.cancel()
        val transactionData = transactionData

        if (transactionData != null) {
            gasPriceInfoJob = coroutineScope.launch {
                try {
                    val transaction = feeData(gasPriceInfo, transactionData)
                    sync(transaction)
                } catch (e: CancellationException) {
                    // do nothing
                } catch (e: Throwable) {
                    _transactionStatusFlow.tryEmit(DataState.Error(e))
                }
            }
        } else {
            _transactionStatusFlow.tryEmit(DataState.Loading)
        }
    }

    private suspend fun feeData(
        gasPriceInfo: GasPriceInfo,
        transactionData: TransactionData
    ): Transaction {
        val gasPrice = gasPriceInfo.gasPrice
        val gasPriceDefault = gasPriceInfo.gasPriceDefault
        val default = gasPriceInfo.default
        val warnings = gasPriceInfo.warnings
        val errors = gasPriceInfo.errors

        // Estimating a transfer that already exceeds the balance only earns an RPC refusal,
        // whose wording decides whether the user sees a real reason or a raw node message. Only
        // a balance that has actually loaded can refuse it: before that the estimate goes ahead,
        // exactly as it did before this shortcut existed.
        knownEvmBalance?.let { balance ->
            if (transactionData.value > balance) {
                throw FeeSettingsError.InsufficientBalance
            }
        }

        return if (transactionData.input.isEmpty() && transactionData.value == evmBalance) {
            val gasData = gasData(gasPrice, gasPriceDefault, BigInteger.ONE, transactionData)
            val adjustedValue = transactionData.value - gasData.fee
            if (adjustedValue <= BigInteger.ZERO) {
                throw FeeSettingsError.InsufficientBalance
            } else {
                val transactionDataAdjusted = TransactionData(transactionData.to, adjustedValue, byteArrayOf())
                Transaction(transactionDataAdjusted, gasData, default, warnings, errors)
            }
        } else {
            val gasData = gasData(gasPrice, gasPriceDefault, null, transactionData)
            Transaction(transactionData, gasData, default, warnings, errors)
        }
    }

    private suspend fun gasData(
        gasPrice: GasPrice,
        gasPriceDefault: GasPrice,
        stubAmount: BigInteger? = null,
        transactionData: TransactionData
    ): GasData {
        val gasLimit = gasLimit

        if (gasLimit != null) {
            return GasData(gasLimit = gasLimit, gasPrice = gasPrice)
        }

        return try {
            gasDataService.estimatedGasData(gasPrice, transactionData, stubAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (error: Throwable) {
            if (error.convertedError == EvmError.LowerThanBaseGasLimit) {
                gasDataService.estimatedGasData(gasPriceDefault, transactionData, stubAmount).also {
                    it.gasPrice = gasPrice
                }
            } else {
                throw error
            }
        }
    }

    private fun sync(transaction: Transaction) {
        _transactionStatusFlow.tryEmit(
            if (transaction.totalAmount > evmBalance) {
                DataState.Success(transaction.copy(errors = transaction.errors + FeeSettingsError.InsufficientBalance))
            } else {
                DataState.Success(transaction)
            }
        )
    }

    fun setGasLimit(gasLimit: Long?) {
        this.gasLimit = gasLimit
        sync()
    }

    fun setTransactionData(transactionData: TransactionData) {
        this.transactionData = transactionData
        sync()
    }

}
