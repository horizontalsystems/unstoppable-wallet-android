package io.horizontalsystems.walletkit.modules.evmfee.eip1559

import io.horizontalsystems.walletkit.core.Warning
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.evmfee.Bound
import io.horizontalsystems.walletkit.modules.evmfee.FeeSettingsError
import io.horizontalsystems.walletkit.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.walletkit.modules.evmfee.GasPriceInfo
import io.horizontalsystems.walletkit.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.FeePriority
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.FeeHistory
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.GasPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.cancel

class Eip1559GasPriceService(
    private val gasProvider: Eip1559GasPriceProvider,
    private val refreshSignalFlow: Flow<Long>,
    minGasPrice: GasPrice.Eip1559? = null,
    private val initialGasPrice: GasPrice.Eip1559? = null,
    private val feePriority: FeePriority = FeePriority.Standard,
) : IEvmGasPriceService() {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val blocksCount: Long = 10
    private val rewardPercentile = rewardPercentiles(feePriority)
    private val lastNRecommendedBaseFees = 2

    private val minBaseFee: Long? = minGasPrice?.let { it.maxFeePerGas - it.maxPriorityFeePerGas }
    private val minPriorityFee: Long? = minGasPrice?.maxPriorityFeePerGas

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

    override fun clear() {
        coroutineScope.cancel()
    }

    override fun start() {
        if (initialGasPrice != null) {
            setGasPrice(initialGasPrice.maxFeePerGas, initialGasPrice.maxPriorityFeePerGas)
        } else {
            setRecommended()
        }

        coroutineScope.launch {
            refreshSignalFlow.collect {
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
            val feeHistory = gasProvider.feeHistory(blocksCount, DefaultBlockParameter.Latest, rewardPercentile)
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

        val recommendedPriorityFee = max(recommendedPriorityFee(feeHistory, feePriority), minPriorityFee ?: 0)
        currentPriorityFee = recommendedPriorityFee

        val newRecommendGasPrice = recommendedGasPrice(recommendedBaseFee, recommendedPriorityFee)

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

    companion object {
        private const val STANDARD_REWARD_PERCENTILE = 50
        private const val PRIORITY_REWARD_PERCENTILE = 75
        private const val BASE_FEE_SURCHARGE_PERCENT = 125

        /**
         * The max fee leaves room for the base fee to rise before the transaction is included.
         * The tip does not change between blocks, so it is added as it is.
         */
        fun recommendedGasPrice(baseFee: Long, priorityFee: Long) = GasPrice.Eip1559(
            maxFeePerGas = baseFee * BASE_FEE_SURCHARGE_PERCENT / 100 + priorityFee,
            maxPriorityFeePerGas = priorityFee,
        )

        fun rewardPercentiles(feePriority: FeePriority): List<Int> = when (feePriority) {
            FeePriority.Standard -> listOf(STANDARD_REWARD_PERCENTILE)
            FeePriority.Priority -> listOf(STANDARD_REWARD_PERCENTILE, PRIORITY_REWARD_PERCENTILE)
        }

        /**
         * The standard tip is the mean of the 50th-percentile rewards over all blocks. The
         * priority tip is the median of the 75th-percentile rewards (the second column, as
         * requested by [rewardPercentiles]) over the blocks that used gas, never below the
         * standard tip, and the standard tip when no block qualifies.
         */
        fun recommendedPriorityFee(feeHistory: FeeHistory, feePriority: FeePriority): Long {
            val standardTips = feeHistory.reward.mapNotNull { it.firstOrNull() }
            val standardTip = if (standardTips.isEmpty()) 0 else standardTips.sum() / standardTips.size

            if (feePriority != FeePriority.Priority) return standardTip

            val priorityTips = feeHistory.reward.zip(feeHistory.gasUsedRatio)
                .mapNotNull { (row, ratio) -> row.getOrNull(1).takeIf { ratio > 0 } }

            return median(priorityTips)?.let { max(it, standardTip) } ?: standardTip
        }

        private fun median(values: List<Long>): Long? {
            if (values.isEmpty()) return null

            val sorted = values.sorted()
            val middle = sorted.size / 2

            return if (sorted.size % 2 == 0) mean(sorted[middle - 1], sorted[middle]) else sorted[middle]
        }

        // Not (a + b) / 2: node-supplied rewards can be close to Long.MAX_VALUE.
        private fun mean(a: Long, b: Long): Long = a / 2 + b / 2 + (a % 2 + b % 2) / 2
    }
}
