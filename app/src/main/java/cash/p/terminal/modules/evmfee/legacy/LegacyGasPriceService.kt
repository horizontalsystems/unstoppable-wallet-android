package cash.p.terminal.modules.evmfee.legacy

import cash.p.terminal.core.Warning
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.evmfee.Bound
import cash.p.terminal.modules.evmfee.FeeSettingsWarning
import cash.p.terminal.modules.evmfee.GasPriceInfo
import cash.p.terminal.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val recommendedGasPriceSelected = MutableStateFlow(true)
    override val recommendedGasPriceSelectedFlow: StateFlow<Boolean>
        get() = recommendedGasPriceSelected.asStateFlow()

    init {
        if (initialGasPrice != null) {
            setGasPrice(initialGasPrice)
        } else {
            setRecommended()
        }
    }

    override fun setRecommended() {
        recommendedGasPriceSelected.update { true }

        state = DataState.Loading
        disposable?.dispose()

        recommendedGasPriceSingle
            .subscribeIO({ recommended ->
                state = DataState.Success(
                    GasPriceInfo(
                        gasPrice = GasPrice.Legacy(recommended),
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
        recommendedGasPriceSelected.update { false }

        state = DataState.Loading
        disposable?.dispose()

        recommendedGasPriceSingle
            .subscribeIO({ recommended ->
                val warnings = mutableListOf<Warning>()
                val errors = mutableListOf<Throwable>()

                if (value < riskOfStuckBound.calculate(recommended)) {
                    warnings.add(FeeSettingsWarning.RiskOfGettingStuck)
                }

                if (value >= overpricingBound.calculate(recommended)) {
                    warnings.add(FeeSettingsWarning.Overpricing)
                }

                state = DataState.Success(GasPriceInfo(GasPrice.Legacy(value), warnings, errors))
            }, {
                state = DataState.Error(it)
            }).let {
                disposable = it
            }
    }
}
