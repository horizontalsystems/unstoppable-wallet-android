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

/**
 * Adapter for a NEP-141 token. No activation is needed to receive: whoever sends the token pays
 * the receiver's storage registration, and so does this adapter when it sends to an
 * unregistered account.
 */
class NearTokenAdapter(
    kitWrapper: NearKitWrapper,
    val contractId: String,
    private val decimals: Int,
) : BaseNearAdapter(kitWrapper) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var tokenBalance: BigDecimal = BigDecimal.ZERO

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlow: Flow<Unit> get() = _balanceStateUpdatedFlow
    override val balanceData: BalanceData get() = BalanceData(tokenBalance)
    override val balanceUpdatedFlow: Flow<Unit> get() = _balanceUpdatedFlow

    override var fee: BigDecimal = DEFAULT_FEE
        private set

    override fun start() {
        kitWrapper.watchToken(contractId)
        coroutineScope.launch {
            kit.getFtBalanceFlow(contractId).collectSafely { balance ->
                tokenBalance = balance?.let { BigDecimal(it).movePointLeft(decimals) } ?: BigDecimal.ZERO
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            kit.syncStateFlow.collectSafely {
                balanceState = it.toAdapterState()
                _balanceStateUpdatedFlow.tryEmit(Unit)
            }
        }
    }

    override fun stop() {
        kitWrapper.unwatchToken(contractId)
        coroutineScope.cancel()
    }

    override fun refresh() {
        kit.refresh()
    }

    // ISendNearAdapter

    override val maxSendableBalance: BigDecimal
        get() = tokenBalance

    override suspend fun estimate(address: String, amount: BigDecimal, memo: String?): NearSendEstimate {
        val estimate = kit.estimateFtTransfer(contractId, address, toUnits(amount), memo)
        fee = NearAmount.toNear(estimate.fee.fee)
        val storageDeposit = estimate.storageDeposit?.let { NearAmount.toNear(it) }
        return NearSendEstimate(
            fee = fee,
            storageDeposit = storageDeposit,
            // NEP-141 transfers attach one yoctoNEAR, which is below display precision
            requiredNear = NearAmount.toNear(estimate.fee.requiredBalance) + (storageDeposit ?: BigDecimal.ZERO),
        )
    }

    override suspend fun send(amount: BigDecimal, address: String, memo: String?): String =
        kit.sendFt(contractId, address, toUnits(amount), memo).hash

    private fun toUnits(amount: BigDecimal) = amount.movePointRight(decimals).toBigIntegerExact()
}
