package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsError
import io.horizontalsystems.bankwallet.modules.evmfee.GasData
import io.horizontalsystems.bankwallet.modules.evmfee.GasDataError
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.Transaction
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.OneInchSwapParameters
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchKitHelper
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.oneinchkit.Swap
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class OneInchFeeService(
    private val oneInchKitHelper: OneInchKitHelper,
    private val evmKit: EthereumKit,
    private val gasPriceService: IEvmGasPriceService,
    parameters: OneInchSwapParameters,
) : IEvmFeeService {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val disposable = CompositeDisposable()
    private var gasPriceInfoDisposable: Disposable? = null

    private var retryDelayTimeInSeconds = 3L
    private var retryDisposable: Disposable? = null

    private val evmBalance: BigInteger
        get() = evmKit.accountState?.balance ?: BigInteger.ZERO

    var parameters: OneInchSwapParameters = parameters
        private set

    private val _transactionStatusFlow = MutableStateFlow<DataState<Transaction>>(DataState.Error(GasDataError.NoTransactionData))
    override val transactionStatusFlow = _transactionStatusFlow.asStateFlow()

    init {
        coroutineScope.launch {
            gasPriceService.stateFlow
                .transformWhile { gasPriceServiceState ->
                    emit(gasPriceServiceState)
                    gasPriceServiceState.dataOrNull == null
                }
                .collect {
                    sync(it)
                }
        }
    }

    override fun reset() {
        gasPriceService.setRecommended()
    }

    override fun clear() {
        coroutineScope.cancel()
        disposable.clear()
        gasPriceInfoDisposable?.dispose()
        retryDisposable?.dispose()
    }

    private fun sync(gasPriceServiceState: DataState<GasPriceInfo>) {
        when (gasPriceServiceState) {
            is DataState.Error -> {
                _transactionStatusFlow.update { gasPriceServiceState }
            }
            DataState.Loading -> {
                _transactionStatusFlow.update { DataState.Loading }
            }
            is DataState.Success -> {
                sync(gasPriceServiceState.data)
            }
        }
    }

    private fun sync(gasPriceInfo: GasPriceInfo) {
        gasPriceInfoDisposable?.dispose()
        retryDisposable?.dispose()

        oneInchKitHelper.getSwapAsync(
            fromToken = parameters.tokenFrom,
            toToken = parameters.tokenTo,
            fromAmount = parameters.amountFrom,
            recipient = parameters.recipient?.hex,
            slippagePercentage = parameters.slippage.toFloat(),
            gasPrice = gasPriceInfo.gasPrice
        )
            .subscribeIO({ swap ->
                sync(swap, gasPriceInfo)
            }, { error ->
                onError(error, gasPriceInfo)
            })
            .let { gasPriceInfoDisposable = it }
    }

    private fun sync(swap: Swap, gasPriceInfo: GasPriceInfo) {
        val swapTx = swap.transaction
        val gasData = GasData(
            gasLimit = swapTx.gasLimit,
            gasPrice = gasPriceInfo.gasPrice
        )

        parameters = parameters.copy(
            amountTo = swap.toTokenAmount.toBigDecimal().movePointLeft(swap.toToken.decimals).stripTrailingZeros()
        )

        val transactionData = TransactionData(swapTx.to, swapTx.value, swapTx.data)
        val transaction = Transaction(transactionData, gasData, gasPriceInfo.default, gasPriceInfo.warnings, gasPriceInfo.errors)

        _transactionStatusFlow.update {
            if (transaction.totalAmount > evmBalance) {
                DataState.Success(
                    transaction.copy(
                        warnings = gasPriceInfo.warnings,
                        errors = gasPriceInfo.errors + FeeSettingsError.InsufficientBalance
                    )
                )
            } else {
                DataState.Success(transaction)
            }
        }
    }

    private fun onError(error: Throwable, gasPriceInfo: GasPriceInfo) {
        parameters = parameters.copy(amountTo = BigDecimal.ZERO)
        _transactionStatusFlow.update { DataState.Error(error) }

        if (error is EvmError.CannotEstimateSwap) {
            retryDisposable = Single.timer(retryDelayTimeInSeconds, TimeUnit.SECONDS)
                .subscribeIO {
                    sync(gasPriceInfo)
                }
        }
    }
}
