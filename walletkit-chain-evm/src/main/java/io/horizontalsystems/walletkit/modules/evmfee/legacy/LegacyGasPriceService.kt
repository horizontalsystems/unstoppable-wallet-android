package io.horizontalsystems.walletkit.modules.evmfee.legacy

import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.evmfee.Bound
import io.horizontalsystems.walletkit.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.walletkit.modules.evmfee.GasPriceInfo
import io.horizontalsystems.walletkit.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.FeePriority
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.coroutines.cancel

class LegacyGasPriceService(
    private val gasPriceProvider: LegacyGasPriceProvider,
    private val minRecommendedGasPrice: Long? = null,
    private val initialGasPrice: Long? = null,
    private val feePriority: FeePriority = FeePriority.Standard,
) : IEvmGasPriceService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var setGasPriceJob: Job? = null
    private var recommendedGasPrice: Long? = null

    private val overpricingBound = Bound.Multiplied(BigDecimal(1.5))
    private val riskOfStuckBound = Bound.Multiplied(BigDecimal(0.9))

    private var state: DataState<GasPriceInfo> = DataState.Loading

    override fun createState() = state

    override fun clear() {
        coroutineScope.cancel()
    }

    override fun start() {
        if (initialGasPrice != null) {
            setGasPrice(initialGasPrice)
        } else {
            setRecommended()
        }
    }

    private suspend fun getRecommendedGasPriceSingle(): Long {
        recommendedGasPrice?.let {
            return it
        }

        val gasPrice = prioritized(gasPriceProvider.gasPrice(), feePriority)
        val adjustedGasPrice = gasPrice.coerceAtLeast(minRecommendedGasPrice ?: 0)

        recommendedGasPrice = adjustedGasPrice

        return adjustedGasPrice
    }

    override fun setRecommended() {
        setGasPriceInternal(null)
    }

    fun setGasPrice(value: Long) {
        setGasPriceInternal(value)
    }

    private fun setGasPriceInternal(value: Long?) {
        setGasPriceJob?.cancel()
        setGasPriceJob = coroutineScope.launch {
            try {
                val recommended = getRecommendedGasPriceSingle()

                val gasPriceInfo = if (value == null) {
                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(recommended),
                        gasPriceDefault = GasPrice.Legacy(recommended),
                        default = true,
                        warnings = listOf<FeeSettingsWarning>(),
                        errors = listOf()
                    )
                } else {
                    val warnings = buildList {
                        if (value < riskOfStuckBound.calculate(recommended)) {
                            add(FeeSettingsWarning.RiskOfGettingStuckLegacy)
                        }

                        if (value >= overpricingBound.calculate(recommended)) {
                            add(FeeSettingsWarning.Overpricing)
                        }
                    }

                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(value),
                        gasPriceDefault = GasPrice.Legacy(recommended),
                        default = false,
                        warnings = warnings,
                        errors = listOf()
                    )
                }

                state = DataState.Success(gasPriceInfo)
                emitState()
            } catch (e: Throwable) {
                state = DataState.Error(e)
                emitState()
            }
        }
    }

    companion object {
        private const val PRIORITY_PERCENT = 120L

        /** [gasPrice] raised by 20% and rounded up for [FeePriority.Priority]. */
        fun prioritized(gasPrice: Long, feePriority: FeePriority): Long {
            // A node-supplied price that would overflow when scaled is used as it is.
            val scalable = gasPrice in 1..(Long.MAX_VALUE - 99) / PRIORITY_PERCENT

            return if (feePriority == FeePriority.Priority && scalable) {
                (gasPrice * PRIORITY_PERCENT + 99) / 100
            } else {
                gasPrice
            }
        }
    }
}
