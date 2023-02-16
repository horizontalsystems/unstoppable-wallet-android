package io.horizontalsystems.bankwallet.modules.evmfee.legacy

import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.Bound
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.lang.Long.max
import java.math.BigDecimal

class LegacyGasPriceService(
    private val gasPriceProvider: LegacyGasPriceProvider,
    private val minRecommendedGasPrice: Long? = null,
    initialGasPrice: Long? = null,
) : IEvmGasPriceService {

    var recommendedGasPrice: Long? = null
    private var disposable: Disposable? = null

    private val overpricingBound = Bound.Multiplied(BigDecimal(1.5))
    private val riskOfStuckBound = Bound.Multiplied(BigDecimal(0.9))

    override var state: DataState<GasPriceInfo> = DataState.Loading
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private val stateSubject = PublishSubject.create<DataState<GasPriceInfo>>()
    override val stateObservable: Observable<DataState<GasPriceInfo>>
        get() = stateSubject

    private val recommendedGasPriceSingle
        get() = recommendedGasPrice?.let { Single.just(it) }
            ?: gasPriceProvider.gasPriceSingle()
                .map { it }
                .doOnSuccess { gasPrice ->
                    val adjustedGasPrice = max(gasPrice.toLong(), minRecommendedGasPrice ?: 0)
                    recommendedGasPrice = adjustedGasPrice
                }

    init {
        if (initialGasPrice != null) {
            setGasPrice(initialGasPrice)
        } else {
            setRecommended()
        }
    }

    override fun setRecommended() {
        state = DataState.Loading
        disposable?.dispose()

        recommendedGasPriceSingle
            .subscribeIO({ recommended ->
                state = DataState.Success(
                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(recommended),
                        gasPriceDefault = GasPrice.Legacy(recommended),
                        default = true,
                        warnings = listOf(),
                        errors = listOf()
                    )
                )
            }, {
                state = DataState.Error(it)
            }).let {
                disposable = it
            }
    }

    fun setGasPrice(value: Long) {
        state = DataState.Loading
        disposable?.dispose()

        recommendedGasPriceSingle
            .subscribeIO({ recommended ->
                val warnings = mutableListOf<Warning>()
                val errors = mutableListOf<Throwable>()

                if (value < riskOfStuckBound.calculate(recommended)) {
                    warnings.add(FeeSettingsWarning.RiskOfGettingStuckLegacy)
                }

                if (value >= overpricingBound.calculate(recommended)) {
                    warnings.add(FeeSettingsWarning.Overpricing)
                }

                state = DataState.Success(
                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(value),
                        gasPriceDefault = GasPrice.Legacy(recommended),
                        default = false,
                        warnings = warnings,
                        errors = errors
                    )
                )
            }, {
                state = DataState.Error(it)
            }).let {
                disposable = it
            }
    }
}
