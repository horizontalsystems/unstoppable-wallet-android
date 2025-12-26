package io.horizontalsystems.bankwallet.modules.evmfee.eip1559

import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.Bound
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsError
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.FeeHistory
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Flowable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

class Eip1559GasPriceService(
    private val gasProvider: Eip1559GasPriceProvider,
    private val refreshSignalFlowable: Flowable<Long>,
    minGasPrice: GasPrice.Eip1559? = null,
    initialGasPrice: GasPrice.Eip1559? = null
) : IEvmGasPriceService() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val blocksCount: Long = 10
    private val rewardPercentile = listOf(50)
    private val lastNRecommendedBaseFees = 2

    private val minBaseFee: Long? = minGasPrice?.let { it.maxFeePerGas - it.maxPriorityFeePerGas }
    private val minPriorityFee: Long? = minGasPrice?.maxPriorityFeePerGas
    private val initialBaseFee: Long? = initialGasPrice?.let { it.maxFeePerGas - it.maxPriorityFeePerGas }
    private val initialPriorityFee: Long? = initialGasPrice?.maxPriorityFeePerGas

    private val overpricingBound = Bound.Multiplied(BigDecimal(1.5))
    private val riskOfStuckBound = Bound.Multiplied(BigDecimal(1))

    private var recommendedGasPrice: GasPrice.Eip1559? = null

    private var state: DataState<GasPriceInfo> = DataState.Loading

    override fun createState() = state

    private var recommendedGasPriceSelected = true

    var currentBaseFee: Long? = null
        private set

    var currentPriorityFee: Long? = null
        private set

    constructor(
        gasProvider: Eip1559GasPriceProvider,
        evmKit: EthereumKit,
        minGasPrice: GasPrice.Eip1559? = null,
        initialGasPrice: GasPrice.Eip1559? = null
    ) : this(gasProvider, evmKit.lastBlockHeightFlowable, minGasPrice, initialGasPrice)

    override fun start() {
        if (initialBaseFee != null && initialPriorityFee != null) {
            setGasPrice(initialBaseFee, initialPriorityFee)
        } else {
            setRecommended()
        }

        coroutineScope.launch {
            refreshSignalFlowable.asFlow().collect {
                syncRecommended()
            }
        }
    }

    override fun setRecommended() {
        recommendedGasPriceSelected = true

        val recommendedGasPrice = recommendedGasPrice

        if (recommendedGasPrice == null) {
            coroutineScope.launch {
                syncRecommended()
            }
        } else {
            state = DataState.Success(
                GasPriceInfo(
                    gasPrice = recommendedGasPrice,
                    gasPriceDefault = recommendedGasPrice,
                    default = true,
                    warnings = listOf(),
                    errors = listOf()
                )
            )

            emitState()
        }
    }

    fun setGasPrice(maxFee: Long, priorityFee: Long) {
        recommendedGasPriceSelected = false

        val newGasPrice = GasPrice.Eip1559(maxFee, priorityFee)
        state = validatedGasPriceInfoState(newGasPrice)

        emitState()
    }

    private fun validatedGasPriceInfoState(gasPrice: GasPrice): DataState<GasPriceInfo> {
        return try {
            DataState.Success(validatedGasPriceInfo(gasPrice))
        } catch (error: Throwable) {
            DataState.Error(error)
        }
    }

    @Throws
    private fun validatedGasPriceInfo(gasPrice: GasPrice): GasPriceInfo {
        val gasPriceEip1559 = (gasPrice as? GasPrice.Eip1559)
            ?: throw FeeSettingsError.InvalidGasPriceType("Expected EIP1559, received Legacy")

        val recommendedGasPrice = recommendedGasPrice
        val warnings = mutableListOf<Warning>()
        val errors = mutableListOf<Throwable>()

        if (recommendedGasPrice != null) {
            val recommendedBaseFee = recommendedGasPrice.maxFeePerGas - recommendedGasPrice.maxPriorityFeePerGas
            val tip = min(gasPriceEip1559.maxFeePerGas - recommendedBaseFee, gasPriceEip1559.maxPriorityFeePerGas)

            when {
                tip < riskOfStuckBound.calculate(recommendedGasPrice.maxPriorityFeePerGas) -> {
                    warnings.add(FeeSettingsWarning.RiskOfGettingStuck)
                }
                tip > overpricingBound.calculate(recommendedGasPrice.maxPriorityFeePerGas) -> {
                    warnings.add(FeeSettingsWarning.Overpricing)
                }
            }
        }

        return GasPriceInfo(
            gasPrice = gasPriceEip1559,
            gasPriceDefault = recommendedGasPrice ?: gasPriceEip1559,
            default = recommendedGasPriceSelected,
            warnings = warnings,
            errors = errors
        )
    }

    private suspend fun syncRecommended() {
        try {
            val feeHistory = gasProvider.feeHistorySingle(blocksCount, DefaultBlockParameter.Latest, rewardPercentile).await()
            handle(feeHistory)
        } catch (error: Throwable) {
            handle(error)
        }
    }

    private fun handle(error: Throwable) {
        currentBaseFee = null
        currentPriorityFee = null
        state = DataState.Error(error)

        emitState()
    }

    private fun handle(feeHistory: FeeHistory) {
        val recommendedBaseFee = max(recommendedBaseFee(feeHistory), minBaseFee ?: 0)
        currentBaseFee = recommendedBaseFee

        val recommendedPriorityFee = max(recommendedPriorityFee(feeHistory), minPriorityFee ?: 0)
        currentPriorityFee = recommendedPriorityFee

        val newRecommendGasPrice = GasPrice.Eip1559(recommendedBaseFee + recommendedPriorityFee, recommendedPriorityFee)

        recommendedGasPrice = newRecommendGasPrice

        if (recommendedGasPriceSelected) {
            state = validatedGasPriceInfoState(newRecommendGasPrice)
        } else {
            state.dataOrNull?.let {
                state = validatedGasPriceInfoState(it.gasPrice)
            }
        }

        emitState()
    }

    private fun recommendedBaseFee(feeHistory: FeeHistory): Long {
        val lastNRecommendedBaseFeesList = feeHistory.baseFeePerGas.takeLast(lastNRecommendedBaseFees)
        return lastNRecommendedBaseFeesList.max()
    }

    private fun recommendedPriorityFee(feeHistory: FeeHistory): Long {
        var priorityFeesSum = 0L
        var priorityFeesCount = 0
        feeHistory.reward.forEach { priorityFeeArray ->
            priorityFeeArray.firstOrNull()?.let { priorityFee ->
                priorityFeesSum += priorityFee
                priorityFeesCount += 1
            }
        }
        return if (priorityFeesCount > 0)
            priorityFeesSum / priorityFeesCount
        else
            0
    }
}
