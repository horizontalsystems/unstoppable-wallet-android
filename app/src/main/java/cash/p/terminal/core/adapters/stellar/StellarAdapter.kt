package cash.p.terminal.core.adapters.stellar

import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.managers.StellarKitWrapper
import cash.p.terminal.core.managers.statusInfo
import cash.p.terminal.core.managers.toAdapterState
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.SyncState
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal
import kotlin.collections.filterIsInstance
import kotlin.let
import kotlin.minus

class StellarAdapter(
    stellarKitWrapper: StellarKitWrapper
) : BaseStellarAdapter(stellarKitWrapper), ISendStellarAdapter {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var totalBalance: BigDecimal? = null
    private var minimumBalance: BigDecimal = BigDecimal.ZERO
    private var assets = listOf<StellarAsset.Asset>()

    // Balance: only the balance-sync stream — operations history must not block send.
    override val balanceState: AdapterState
        get() = stellarKit.syncStateFlow.value.toAdapterState()

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = stellarKit.syncStateFlow.map { }

    override val transactionsSyncState: AdapterState
        get() = when (val state = stellarKit.operationsSyncStateFlow.value) {
            is SyncState.Syncing -> AdapterState.SearchingTxs(0)
            is SyncState.NotSynced -> AdapterState.NotSynced(state.error)
            is SyncState.Synced -> AdapterState.Synced
        }

    override val transactionsSyncStateUpdatedFlow: Flow<Unit>
        get() = stellarKit.operationsSyncStateFlow.map { }

    override val balanceData: BalanceData
        get() = BalanceData(
            availableBalance,
            minimumBalance = minimumBalance,
            stellarAssets = assets
        )

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override val balanceUpdatedFlow: Flow<Unit>
        get() = balanceUpdatedSubject.asFlow()

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(StellarAsset.Native).collect { balance ->
                totalBalance = balance?.balance
                minimumBalance = balance?.minBalance ?: BigDecimal.ZERO
                balanceUpdatedSubject.onNext(Unit)
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

    override suspend fun refresh() {
        stellarKit.refresh()
    }

    override val debugInfo = "debugInfo"
    override val statusInfo: Map<String, Any>
        get() = stellarKit.statusInfo()

    private val availableBalance: BigDecimal
        get() = totalBalance?.let { it - minimumBalance } ?: BigDecimal.ZERO

    override val fee: StateFlow<BigDecimal> = MutableStateFlow(stellarKit.sendFee)

    override val maxSpendableBalance: BigDecimal
        get() = availableBalance - fee.value

    override suspend fun getMinimumSendAmount(address: String) = when {
        !stellarKit.doesAccountExist(address) -> BigDecimal.ONE
        else -> null
    }

    override suspend fun send(amount: BigDecimal, address: String, memo: String?): String? {
        return if (stellarKit.doesAccountExist(address)) {
            stellarKit.sendNative(address, amount, memo).hash
        } else {
            stellarKit.createAccount(address, amount, memo)
            null
        }
    }

    override fun validate(address: String) {
        StellarKit.validateAddress(address)
    }
}
