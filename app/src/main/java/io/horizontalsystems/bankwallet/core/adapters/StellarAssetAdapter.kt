package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toAdapterState
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class StellarAssetAdapter(
    stellarKitWrapper: StellarKitWrapper,
    code: String,
    issuer: String
) : BaseStellarAdapter(stellarKitWrapper), ISendStellarAdapter {

    private val stellarAsset = StellarAsset.Asset(code, issuer)
    private var assetBalance: BigDecimal? = null

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val balance: BigDecimal
        get() = assetBalance ?: BigDecimal.ZERO

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(stellarAsset).collect { balance ->
                assetBalance = balance
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
    }

    override val availableBalance: BigDecimal
        get() = balance
    override val fee: BigDecimal
        get() = stellarKit.sendFee

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        stellarKit.sendAsset(stellarAsset.id, address, amount, memo)
    }

    override suspend fun isActivationRequired() : Boolean {
        return !stellarKit.isAssetEnabled(stellarAsset)
    }

    override fun activate() {
        stellarKit.enableAsset(stellarAsset.id, null)
    }
}
