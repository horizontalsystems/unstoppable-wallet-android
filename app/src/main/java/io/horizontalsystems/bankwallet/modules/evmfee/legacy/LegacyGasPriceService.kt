package io.horizontalsystems.bankwallet.modules.evmfee.legacy

import android.util.Log
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.ICustomRangedFeeProvider
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.bankwallet.modules.evmfee.GasPrice
import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.send.submodules.fee.CustomPriorityUnit
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class LegacyGasPriceService(
    val feeRateProvider: ICustomRangedFeeProvider,
    gasPrice: Long? = null
) : IEvmGasPriceService {

    private var recommendedGasPrice: Long? = null
    private var disposable: Disposable? = null

    val gasPriceRange: LongRange = feeRateProvider.customFeeRange

    override var state: DataState<GasPriceInfo> = DataState.Loading
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private val stateSubject = PublishSubject.create<DataState<GasPriceInfo>>()
    override val stateObservable: Observable<DataState<GasPriceInfo>>
        get() = stateSubject

    private val recommendedGasPriceSingle
        get() = recommendedGasPrice?.let {
            Log.e("AAA", "cached recommendedGasPrice: $it")
            Single.just(it)
        } ?: feeRateProvider.feeRate(FeeRatePriority.RECOMMENDED)
            .map { it.toLong() }
            .doOnSuccess { gasPrice ->
                Log.e("AAA", "recommendedGasPrice: $gasPrice")
                recommendedGasPrice = gasPrice.toLong()
            }

    init {
        if (gasPrice != null) {
            setGasPrice(gasPrice)
        } else {
            setRecommended()
        }
    }

    fun setRecommended() {
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
        Log.e("AAA", "setGasPrice: $value")
        state = DataState.Loading
        disposable?.dispose()

        recommendedGasPriceSingle
            .subscribeIO({ recommended ->
                val warnings = mutableListOf<Warning>()
                val errors = mutableListOf<Throwable>()

                val customGasPriceInGwei = CustomPriorityUnit.Gwei.fromBaseUnit(value)
                val recommendedInGwei = CustomPriorityUnit.Gwei.fromBaseUnit(recommended)

                if (customGasPriceInGwei < recommendedInGwei) {
                    warnings.add(FeeSettingsWarning.RiskOfGettingStuck)
                }

                if (customGasPriceInGwei >= recommendedInGwei * 1.5) {
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
