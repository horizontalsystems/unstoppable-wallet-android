package io.horizontalsystems.bankwallet.modules.evmfee.eip1559

import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.*
import io.horizontalsystems.bankwallet.modules.evmfee.FeeRangeConfig.Bound
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.FeeHistory
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

class Eip1559GasPriceService(
    private val gasProvider: Eip1559GasPriceProvider,
    evmKit: EthereumKit,
    minGasPrice: GasPrice.Eip1559? = null,
    initialGasPrice: GasPrice.Eip1559? = null
) : IEvmGasPriceService {

    private val disposable = CompositeDisposable()
    private val blocksCount: Long = 10
    private val rewardPercentile = listOf(50)
    private val lastNRecommendedBaseFees = 2

    private val minBaseFee: Long? = minGasPrice?.let { it.maxFeePerGas - it.maxPriorityFeePerGas }
    private val minPriorityFee: Long? = minGasPrice?.maxPriorityFeePerGas
    private val initialBaseFee: Long? = initialGasPrice?.let { it.maxFeePerGas - it.maxPriorityFeePerGas }
    private val initialPriorityFee: Long? = initialGasPrice?.maxPriorityFeePerGas

    private val baseFeeRangeConfig = FeeRangeConfig(
        lowerBound = Bound.Multiplied(BigDecimal(0.5)),
        upperBound = Bound.Multiplied(BigDecimal(3.0))
    )
    private val priorityFeeRangeConfig = FeeRangeConfig(
        lowerBound = Bound.Fixed(0),
        upperBound = Bound.Multiplied(BigDecimal(10))
    )
    private val overpricingBound = Bound.Multiplied(BigDecimal(1.5))
    private val riskOfStuckBound = Bound.Multiplied(BigDecimal(0.9))

    private var recommendedGasPrice: GasPrice.Eip1559? = null

    override var state: DataState<GasPriceInfo> = DataState.Loading
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private val stateSubject = PublishSubject.create<DataState<GasPriceInfo>>()
    override val stateObservable: Observable<DataState<GasPriceInfo>>
        get() = stateSubject

    override var isRecommendedGasPriceSelected = true
        private set

    var currentBaseFee: Long? = null
        private set

    var currentPriorityFee: Long? = null
        private set

    val defaultBaseFeeRange: LongRange = 1_000_000_000..100_000_000_000
    var baseFeeRange: LongRange? = null
        private set

    val defaultPriorityFeeRange: LongRange = 1_000_000_000..100_000_000_000
    var priorityFeeRange: LongRange? = null
        private set

    init {
        if (initialBaseFee != null && initialPriorityFee != null) {
            setGasPrice(initialBaseFee, initialPriorityFee)
        } else {
            setRecommended()
        }

        evmKit.lastBlockHeightFlowable
            .subscribeIO {
                syncRecommended()
            }
            .let { disposable.add(it) }
    }

    fun setRecommended() {
        isRecommendedGasPriceSelected = true

        recommendedGasPrice?.let {
            state = DataState.Success(GasPriceInfo(it, listOf(), listOf()))
        } ?: syncRecommended()
    }

    fun setGasPrice(baseFee: Long, maxPriorityFee: Long) {
        isRecommendedGasPriceSelected = false

        val newGasPrice = GasPrice.Eip1559(baseFee + maxPriorityFee, maxPriorityFee)
        state = validatedGasPriceInfoState(newGasPrice)
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
                tip < 0 -> {
                    errors.add(FeeSettingsError.LowMaxFee)
                }
                tip <= riskOfStuckBound.calculate(recommendedGasPrice.maxPriorityFeePerGas) -> {
                    warnings.add(FeeSettingsWarning.RiskOfGettingStuck)
                }
                tip >= overpricingBound.calculate(recommendedGasPrice.maxPriorityFeePerGas) -> {
                    warnings.add(FeeSettingsWarning.Overpricing)
                }
            }
        }

        return GasPriceInfo(gasPriceEip1559, warnings, errors)
    }

    private fun syncRecommended() {
        gasProvider.feeHistorySingle(blocksCount, DefaultBlockParameter.Latest, rewardPercentile)
            .subscribeIO({ feeHistory ->
                handle(feeHistory)
            }, { error ->
                handle(error)
            })
            .let { disposable.add(it) }
    }

    private fun handle(error: Throwable) {
        currentBaseFee = null
        currentPriorityFee = null
        state = DataState.Error(error)
    }

    private fun handle(feeHistory: FeeHistory) {
        val recommendedBaseFee = max(recommendedBaseFee(feeHistory), minBaseFee ?: 0)
        currentBaseFee = recommendedBaseFee

        val recommendedPriorityFee = max(recommendedPriorityFee(feeHistory), minPriorityFee ?: 0)
        currentPriorityFee = recommendedPriorityFee

        val newRecommendGasPrice = GasPrice.Eip1559(recommendedBaseFee + recommendedPriorityFee, recommendedPriorityFee)

        syncFeeRanges(newRecommendGasPrice)

        recommendedGasPrice = newRecommendGasPrice

        if (isRecommendedGasPriceSelected) {
            state = validatedGasPriceInfoState(newRecommendGasPrice)
        } else {
            state.dataOrNull?.let {
                state = validatedGasPriceInfoState(it.gasPrice)
            }
        }
    }

    private fun recommendedBaseFee(feeHistory: FeeHistory): Long {
        val lastNRecommendedBaseFeesList = feeHistory.baseFeePerGas.takeLast(lastNRecommendedBaseFees)
        return java.util.Collections.max(lastNRecommendedBaseFeesList)
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
        val priorityFee = if (priorityFeesCount > 0)
            priorityFeesSum / priorityFeesCount
        else
            0

        return priorityFee
    }

    private fun syncFeeRanges(newRecommendGasPrice: GasPrice.Eip1559) {
        val recommendedBaseFee = newRecommendGasPrice.maxFeePerGas - newRecommendGasPrice.maxPriorityFeePerGas
        val recommendedPriorityFee = newRecommendGasPrice.maxPriorityFeePerGas

        baseFeeRange = getAdjustedFeeRange(
            baseFeeRange,
            recommendedBaseFee,
            baseFeeRangeConfig
        )

        priorityFeeRange = getAdjustedFeeRange(
            priorityFeeRange,
            recommendedPriorityFee,
            priorityFeeRangeConfig
        )
    }

    private fun getAdjustedFeeRange(
        feeRange: LongRange?,
        recommendedFee: Long,
        config: FeeRangeConfig
    ): LongRange {
        val baseFeeRangeLowerBound = if (feeRange == null || feeRange.first > recommendedFee) {
            config.lowerBound.calculate(recommendedFee)
        } else {
            feeRange.first
        }

        val baseFeeRangeUpperBound = if (feeRange == null || feeRange.last < recommendedFee) {
            config.upperBound.calculate(recommendedFee)
        } else {
            feeRange.last
        }

        return baseFeeRangeLowerBound..baseFeeRangeUpperBound
    }

}
