package io.horizontalsystems.bankwallet.modules.evmfee.legacy

import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.Bound
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class LegacyGasPriceService(
    private val gasPriceProvider: LegacyGasPriceProvider,
    private val minRecommendedGasPrice: Long? = null,
    initialGasPrice: Long? = null,
) : IEvmGasPriceService() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var setGasPriceJob: Job? = null
    private var recommendedGasPrice: Long? = null

    private val overpricingBound = Bound.Multiplied(BigDecimal(1.5))
    private val riskOfStuckBound = Bound.Multiplied(BigDecimal(0.9))

    private var state: DataState<GasPriceInfo> = DataState.Loading

    override fun createState() = state

    init {
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

        val gasPrice = gasPriceProvider.gasPriceSingle().await()
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
        state = DataState.Loading
        emitState()

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
}
