package io.horizontalsystems.bankwallet.modules.evmfee

import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class EvmFeeService(
    private val evmKit: EthereumKit,
    private val gasPriceService: IEvmGasPriceService,
    private val gasDataService: EvmCommonGasDataService,
    private val transactionData: TransactionData
) : IEvmFeeService {

    private val disposable = CompositeDisposable()
    private var gasPriceInfoDisposable: Disposable? = null

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    override var transactionStatus: DataState<Transaction> = DataState.Error(GasDataError.NoTransactionData)
        private set(value) {
            field = value
            transactionStatusSubject.onNext(value)
        }
    private val transactionStatusSubject = PublishSubject.create<DataState<Transaction>>()
    override val transactionStatusObservable: Observable<DataState<Transaction>> = transactionStatusSubject

    init {
        sync(gasPriceService.state)
        gasPriceService.stateObservable
            .subscribeIO {
                sync(it)
            }.let { disposable.add(it) }
    }

    override fun reset() {
        gasPriceService.setRecommended()
    }

    fun onCleared() {
        disposable.clear()
        gasPriceInfoDisposable?.dispose()
    }

    private fun sync(gasPriceServiceState: DataState<GasPriceInfo>) {
        when (gasPriceServiceState) {
            is DataState.Error -> {
                transactionStatus = gasPriceServiceState
            }
            DataState.Loading -> {
                transactionStatus = DataState.Loading
            }
            is DataState.Success -> {
                sync(gasPriceServiceState.data)
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

        val feeDataSingle = feeDataSingle(gasPrice, gasPriceDefault, default, warnings, errors)

        feeDataSingle
            .subscribeIO({ transaction ->
                sync(transaction)
            }, {
                transactionStatus = DataState.Error(it)
            })
            .let { gasPriceInfoDisposable = it }
    }

    private fun feeDataSingle(
        gasPrice: GasPrice,
        gasPriceDefault: GasPrice,
        default: Boolean,
        warnings: List<Warning>,
        errors: List<Throwable>
    ): Single<Transaction> = if (transactionData.input.isEmpty() && transactionData.value == evmBalance) {
        gasDataSingle(gasPrice, gasPriceDefault, BigInteger.ONE).map { gasData ->
            val adjustedValue = transactionData.value - gasData.fee
            if (adjustedValue <= BigInteger.ZERO) {
                throw FeeSettingsError.InsufficientBalance
            } else {
                val transactionData = TransactionData(transactionData.to, adjustedValue, byteArrayOf())
                Transaction(transactionData, gasData, default, warnings, errors)
            }
        }
    } else {
        gasDataSingle(gasPrice, gasPriceDefault).map { gasData ->
            Transaction(transactionData, gasData, default, warnings, errors)
        }
    }

    private fun gasDataSingle(
        gasPrice: GasPrice,
        gasPriceDefault: GasPrice,
        stubAmount: BigInteger? = null
    ): Single<GasData> = gasDataService.estimatedGasDataAsync(gasPrice, transactionData, stubAmount)
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


    private fun sync(transaction: Transaction) {
        transactionStatus = if (transaction.totalAmount > evmBalance) {
            DataState.Success(transaction.copy(errors = transaction.errors + FeeSettingsError.InsufficientBalance))
        } else {
            DataState.Success(transaction)
        }
    }

}
