package cash.p.terminal.core.managers

import cash.p.terminal.core.isNative
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.isLitecoinMweb
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

class PendingBalanceCalculator(
    private val pendingRepository: PendingTransactionRepository,
    dispatcherProvider: DispatcherProvider
) : Clearable {
    private val pendingCache = ConcurrentHashMap<String, List<PendingTransactionEntity>>()
    private val observingJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private val _pendingChangedFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val pendingChangedFlow: SharedFlow<Unit> = _pendingChangedFlow.asSharedFlow()

    fun startObserving(accountId: String) {
        observingJobs.computeIfAbsent(accountId) {
            scope.launch {
                pendingRepository.getActivePendingFlow(accountId).collect { list ->
                    pendingCache[accountId] = list
                    _pendingChangedFlow.tryEmit(Unit)
                }
            }
        }
    }

    fun onPendingInserted(entity: PendingTransactionEntity) {
        pendingCache.compute(entity.walletId) { _, current ->
            current.orEmpty() + entity
        }
        _pendingChangedFlow.tryEmit(Unit)
    }

    fun stopObserving(accountId: String) {
        observingJobs.remove(accountId)?.cancel()
        pendingCache.remove(accountId)
    }

    override fun clear() {
        scope.cancel()
        observingJobs.clear()
        pendingCache.clear()
    }

    fun adjustBalance(wallet: Wallet, rawBalance: BalanceData): BalanceData {
        val pendingList = pendingCache[wallet.account.id] ?: return rawBalance
        val adjustedAvailable = calculateAdjustedAvailable(pendingList, wallet.token, rawBalance.available)
        return rawBalance.copy(available = adjustedAvailable)
    }

    /**
     * Smart deduction algorithm that automatically handles different SDK behaviors:
     * - TON/EVM: SDK doesn't deduct until confirmed → we apply full deduction
     * - Bitcoin/Zcash: SDK deducts immediately → we apply 0 deduction
     * - Mixed: SDK partially deducted → we apply remaining deduction
     */
    private fun calculateAdjustedAvailable(
        pendingList: List<PendingTransactionEntity>,
        token: Token,
        currentSdkBalance: BigDecimal
    ): BigDecimal {
        val relevantPending = pendingList.filter {
            it.coinUid == token.coin.uid &&
                it.tokenTypeId == token.type.id &&
                it.blockchainTypeUid == token.blockchainType.uid
        }
        if (relevantPending.isEmpty()) return currentSdkBalance

        // Fee is only deducted from native token balance (not ERC-20/TRC-20/Jetton)
        val isNativeToken = token.type.isNative

        // 1. Calculate total pending amount (amount + fee for native tokens only)
        val totalPendingAmount = relevantPending.sumOf { entity ->
            val amount = entity.amountAtomic.toBigDecimal().movePointLeft(token.decimals)
            val fee = if (isNativeToken) {
                entity.feeAtomic?.toBigDecimal()?.movePointLeft(token.decimals)
                    ?: BigDecimal.ZERO
            } else BigDecimal.ZERO
            amount + fee
        }

        // 2. Get baseline SDK balance (max from all pending - earliest "clean" balance)
        val baselineSdkBalance = relevantPending.maxOf { entity ->
            entity.sdkBalanceAtCreationAtomic.toBigDecimal().movePointLeft(token.decimals)
        }

        // 3. How much has SDK already deducted from baseline?
        // If SDK deducts immediately (Bitcoin): baselineSdkBalance - currentSdkBalance ≈ totalPending
        // If SDK doesn't deduct (TON): baselineSdkBalance - currentSdkBalance ≈ 0
        val sdkAlreadyDeducted = (baselineSdkBalance - currentSdkBalance)
            .coerceAtLeast(BigDecimal.ZERO)

        // 4. Our deduction = total pending - what SDK already deducted
        val ourDeduction = (totalPendingAmount - sdkAlreadyDeducted)
            .coerceAtLeast(BigDecimal.ZERO)
        val adjustedAfterDeduction = currentSdkBalance - ourDeduction.coerceAtMost(currentSdkBalance)
        val expectedMwebAvailable = if (token.isLitecoinMweb) {
            (baselineSdkBalance - totalPendingAmount).coerceAtLeast(BigDecimal.ZERO)
        } else {
            null
        }
        val mwebZeroSnapshotFallbackAvailable = expectedMwebAvailable?.takeIf {
            currentSdkBalance.signum() == 0 && it.signum() > 0
        }

        // 5. Check for confirmed TXs to cleanup (per-TX confirmation detection)
        // Collect IDs first to avoid race condition during async deletion
        val idsToDelete = if (expectedMwebAvailable != null) {
            // MWEB replaces spent confirmed inputs with unconfirmed change right after broadcast.
            emptyList()
        } else {
            relevantPending.filter { entity ->
                // Without a tx hash, this pending entry is the only link used to mark
                // the later real transaction as locally created.
                if (entity.txHash.isNullOrBlank()) {
                    return@filter false
                }

                val amount = entity.amountAtomic.toBigDecimal().movePointLeft(token.decimals)
                val fee = if (isNativeToken) {
                    entity.feeAtomic?.toBigDecimal()?.movePointLeft(token.decimals)
                        ?: BigDecimal.ZERO
                } else BigDecimal.ZERO
                val sdkAtCreation = entity.sdkBalanceAtCreationAtomic.toBigDecimal()
                    .movePointLeft(token.decimals)
                val expectedAfterConfirm = sdkAtCreation - amount - fee
                val tolerance = (amount + fee) * BigDecimal("0.05") // 5% tolerance
                (currentSdkBalance - expectedAfterConfirm).abs() <= tolerance
            }.map { it.id }
        }
        if (idsToDelete.isNotEmpty()) {
            scope.launch {
                tryOrNull { pendingRepository.deleteByIds(idsToDelete) }
            }
        }

        return mwebZeroSnapshotFallbackAvailable ?: adjustedAfterDeduction
    }
}
