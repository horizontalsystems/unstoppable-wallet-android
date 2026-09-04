package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.StellarAssetBalance
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.StellarKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
            stellarAssets = assets.map { StellarAssetBalance(it.code) }
        )

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val balanceUpdatedFlow: Flow<Unit>
        get() = _balanceUpdatedFlow
    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = _balanceStateUpdatedFlow

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(StellarAsset.Native).collectSafely { balance ->
                totalBalance = balance?.balance
                minimumBalance = balance?.minBalance ?: BigDecimal.ZERO
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collectSafely {
                balanceState = it.toAdapterState()
                _balanceStateUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.assetBalanceMapFlow.collectSafely {
                assets = it.keys.filterIsInstance<StellarAsset.Asset>()
                _balanceUpdatedFlow.tryEmit(Unit)
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