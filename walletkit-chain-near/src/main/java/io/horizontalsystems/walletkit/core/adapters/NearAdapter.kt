package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.nearkit.models.NearAmount
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.NearSendEstimate
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.NearKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Balance, receive and send adapter for native NEAR. */
class NearAdapter(
    kitWrapper: NearKitWrapper,
) : BaseNearAdapter(kitWrapper) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override var balanceState: AdapterState = AdapterState.Syncing()

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val balanceUpdatedFlow: Flow<Unit> get() = _balanceUpdatedFlow
    override val balanceStateUpdatedFlow: Flow<Unit> get() = _balanceStateUpdatedFlow

    /** Storage staking is locked NEAR: shown as the minimum balance, not as spendable. */
    override val balanceData: BalanceData
        get() {
            val state = kit.accountState
            val locked = NearAmount.toNear(state.amount - state.available)
            return BalanceData(
                available = NearAmount.toNear(state.available),
                minimumBalance = locked,
            )
        }

    override var fee: BigDecimal = DEFAULT_FEE
        private set

    override fun start() {
        coroutineScope.launch {
            kit.accountStateFlow.collectSafely { _balanceUpdatedFlow.tryEmit(Unit) }
        }
        coroutineScope.launch {
            kit.syncStateFlow.collectSafely {
                balanceState = it.toAdapterState()
                _balanceStateUpdatedFlow.tryEmit(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
        kit.refresh()
    }

    // ISendNearAdapter

    override val maxSendableBalance: BigDecimal
        get() = (availableNear - WORST_CASE_TRANSFER_REQUIRED).max(BigDecimal.ZERO)

    override suspend fun estimate(address: String, amount: BigDecimal, memo: String?): NearSendEstimate {
        val estimate = kit.estimateNearTransfer(address)
        fee = NearAmount.toNear(estimate.fee)
        return NearSendEstimate(
            fee = fee,
            storageDeposit = null,
            requiredNear = NearAmount.toNear(estimate.requiredBalance) + amount,
        )
    }

    override suspend fun send(amount: BigDecimal, address: String, memo: String?): String =
        kit.sendNear(address, NearAmount.toYocto(amount)).hash
}
