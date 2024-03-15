package cash.p.terminal.modules.evmfee

import cash.p.terminal.core.EvmError
import cash.p.terminal.core.Warning
import cash.p.terminal.core.convertedError
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger

class EvmFeeService(
    private val evmKit: EthereumKit,
    private val gasPriceService: IEvmGasPriceService,
    private val gasDataService: EvmCommonGasDataService,
    private var transactionData: TransactionData?,
    private var gasLimit: Long? = null,
) : IEvmFeeService {

    private var gasPriceInfoState: DataState<GasPriceInfo> = DataState.Loading
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var gasPriceInfoDisposable: Disposable? = null

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    private val _transactionStatusFlow = MutableStateFlow<DataState<Transaction>>(DataState.Error(GasDataError.NoTransactionData))
    override val transactionStatusFlow = _transactionStatusFlow.asStateFlow()

    init {
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
        gasPriceInfoDisposable?.dispose()
    }

    private fun sync() {
        val gasPriceInfoState = gasPriceInfoState
        when (gasPriceInfoState) {
            is DataState.Error -> {
                _transactionStatusFlow.update { gasPriceInfoState }
            }
            DataState.Loading -> {
                _transactionStatusFlow.update { DataState.Loading }
            }
            is DataState.Success -> {
                sync(gasPriceInfoState.data)
            }
        }
    }

    private fun sync(gasPriceInfo: GasPriceInfo) {
        gasPriceInfoDisposable?.dispose()

        val gasPrice = gasPriceInfo.gasPrice
        val gasPriceDefault = gasPriceInfo.gasPriceDefault
        val default = gasPriceInfo.default
        val warnings = gasPriceInfo.warnings
        val errors = gasPriceInfo.errors

        val transactionData = transactionData

        if (transactionData != null) {
            val feeDataSingle = feeDataSingle(gasPrice, gasPriceDefault, default, warnings, errors,
                transactionData
            )

            feeDataSingle
                .subscribeIO({ transaction ->
                    sync(transaction)
                }, { error ->
                    _transactionStatusFlow.update { DataState.Error(error) }
                })
                .let { gasPriceInfoDisposable = it }
        } else {
            _transactionStatusFlow.update { DataState.Loading }
        }
    }

    private fun feeDataSingle(
        gasPrice: GasPrice,
        gasPriceDefault: GasPrice,
        default: Boolean,
        warnings: List<Warning>,
        errors: List<Throwable>,
        transactionData: TransactionData
    ): Single<Transaction> = if (transactionData.input.isEmpty() && transactionData.value == evmBalance) {
        gasDataSingle(gasPrice, gasPriceDefault, BigInteger.ONE, transactionData).map { gasData ->
            val adjustedValue = transactionData.value - gasData.fee
            if (adjustedValue <= BigInteger.ZERO) {
                throw FeeSettingsError.InsufficientBalance
            } else {
                val transactionData = TransactionData(transactionData.to, adjustedValue, byteArrayOf())
                Transaction(transactionData, gasData, default, warnings, errors)
            }
        }
    } else {
        gasDataSingle(gasPrice, gasPriceDefault, null, transactionData)
            .map { gasData ->
                Transaction(transactionData, gasData, default, warnings, errors)
            }
    }

    private fun gasDataSingle(
        gasPrice: GasPrice,
        gasPriceDefault: GasPrice,
        stubAmount: BigInteger? = null,
        transactionData: TransactionData
    ): Single<GasData> {
        val gasLimit = gasLimit

        if (gasLimit != null) {
            return Single.just(GasData(gasLimit = gasLimit, gasPrice = gasPrice))
        }

        return gasDataService.estimatedGasDataAsync(gasPrice, transactionData, stubAmount)
            .onErrorResumeNext { error ->
                if (error.convertedError == EvmError.LowerThanBaseGasLimit) {
                    gasDataService.estimatedGasDataAsync(gasPriceDefault, transactionData, stubAmount)
                        .map {
                            it.gasPrice = gasPrice
                            it
                        }
                } else {
                    Single.error(error)
                }
            }
    }

    private fun sync(transaction: Transaction) {
        _transactionStatusFlow.update {
            if (transaction.totalAmount > evmBalance) {
                DataState.Success(transaction.copy(errors = transaction.errors + FeeSettingsError.InsufficientBalance))
            } else {
                DataState.Success(transaction)
            }
        }
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
