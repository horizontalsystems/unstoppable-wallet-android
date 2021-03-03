package io.horizontalsystems.bankwallet.modules.swap.allowance

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

class SwapAllowanceService(
        private val spenderAddress: Address,
        private val adapterManager: IAdapterManager,
        ethereumKit: EthereumKit
) {

    private var coin: Coin? = null
    private val stateSubject = PublishSubject.create<Optional<State>>()

    var state: State? = null
        private set(value) {
            if (field != value) {
                field = value
                stateSubject.onNext(Optional.ofNullable(value))
            }
        }
    val stateObservable: Observable<Optional<State>> = stateSubject

    private val disposables = CompositeDisposable()
    private var allowanceDisposable: Disposable? = null

    init {
        ethereumKit.lastBlockHeightFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    sync()
                }
                .let { disposables.add(it) }
    }

    fun set(coin: Coin?) {
        this.coin = coin
        sync()
    }

    fun approveData(dex: SwapModule.Dex, amount: BigDecimal): ApproveData? {
        val allowance = (state as? State.Ready)?.allowance

        return allowance?.let {
            coin?.let { coin ->
                ApproveData(dex, coin, spenderAddress.hex, amount, allowance.value)
            }
        }

    }

    fun onCleared() {
        disposables.clear()
        allowanceDisposable?.dispose()
    }

    private fun sync() {
        allowanceDisposable?.dispose()
        allowanceDisposable = null

        val coin = coin
        val adapter = coin?.let { adapterManager.getAdapterForCoin(it) } as? Eip20Adapter

        if (coin == null || adapter == null) {
            state = null
            return
        }

        if (state != null && state is State.Ready) {
            // no need to set loading, simply update to new allowance value
        } else {
            state = State.Loading
        }

        allowanceDisposable = adapter.allowance(spenderAddress, DefaultBlockParameter.Latest)
                .subscribeOn(Schedulers.io())
                .subscribe({ allowance ->
                    state = State.Ready(CoinValue(coin, allowance))
                }, { error ->
                    state = State.NotReady(error)
                })
    }

    //region models
    sealed class State {
        object Loading : State()
        class Ready(val allowance: CoinValue) : State()
        class NotReady(val error: Throwable) : State()

        override fun equals(other: Any?): Boolean {
            return when {
                this is Loading && other is Loading -> true
                this is Ready && other is Ready -> allowance == other.allowance
                else -> false
            }
        }

        override fun hashCode(): Int {
            return when (this) {
                is Loading -> Loading.javaClass.hashCode()
                is Ready -> allowance.hashCode()
                is NotReady -> error.hashCode()
            }
        }
    }

    @Parcelize
    data class ApproveData(
            val dex: SwapModule.Dex,
            val coin: Coin,
            val spenderAddress: String,
            val amount: BigDecimal,
            val allowance: BigDecimal
    ) : Parcelable

    //endregion
}
