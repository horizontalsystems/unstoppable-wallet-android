package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toAdapterState
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class StellarAdapter(
    stellarKitWrapper: StellarKitWrapper
) : BaseStellarAdapter(stellarKitWrapper) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var totalBalance: BigDecimal? = null
    private var minimumBalance: BigDecimal = BigDecimal.ZERO
    private var assets = listOf<StellarAsset.Asset>()

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceData: BalanceData
        get() = BalanceData(
            availableBalance,
            minimumBalance = minimumBalance,
            stellarAssets = assets
        )

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(StellarAsset.Native).collect { balance ->
                totalBalance = balance?.balance
                minimumBalance = balance?.minBalance ?: BigDecimal.ZERO
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.assetBalanceMapFlow.collect {
                assets = it.keys.filterIsInstance<StellarAsset.Asset>()
                balanceUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
    }

    override val debugInfo = "debugInfo"

    private val availableBalance: BigDecimal
        get() = totalBalance?.let { it - minimumBalance } ?: BigDecimal.ZERO

    override val maxSendableBalance: BigDecimal
        get() = availableBalance - fee

    override val fee: BigDecimal
        get() = stellarKit.sendFee

    override suspend fun getMinimumSendAmount(address: String) = when {
        !stellarKit.doesAccountExist(address) -> BigDecimal.ONE
        else -> null
    }

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        if (stellarKit.doesAccountExist(address)) {
            stellarKit.sendNative(address, amount, memo)
        } else {
            stellarKit.createAccount(address, amount, memo)
        }
    }

    override fun validate(address: String) {
        StellarKit.validateAddress(address)
    }
}