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
import io.horizontalsystems.core.entities.BlockchainType
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
    private companion object {
        val CONFIRMATION_BALANCE_TOLERANCE_RATE = BigDecimal("0.05")
    }

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
        val relevantPending = pendingList.filter { it.matches(token) }
        if (relevantPending.isEmpty()) return currentSdkBalance

        // Fee is only deducted from native token balance (not ERC-20/TRC-20/Jetton)
        val isNativeToken = token.type.isNative

        // 1. Calculate total pending amount (amount + fee for native tokens only)
        val totalPendingAmount = relevantPending.sumOf {
            it.amountWithFee(token.decimals, isNativeToken)
        }

        // 2. Get baseline SDK balance (max from all pending - earliest "clean" balance)
        val baselineSdkBalance = relevantPending.maxOf { entity ->
            entity.sdkBalanceAtCreation(token.decimals)
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
        val isLitecoinMweb = token.isLitecoinMweb
        val expectedMwebAvailable = if (isLitecoinMweb) {
            (baselineSdkBalance - totalPendingAmount).coerceAtLeast(BigDecimal.ZERO)
        } else {
            null
        }
        val mwebZeroSnapshotFallbackAvailable = expectedMwebAvailable?.takeIf {
            currentSdkBalance.signum() == 0 && it.signum() > 0
        }

        cleanupConfirmedPending(
            relevantPending = relevantPending,
            decimals = token.decimals,
            isNativeToken = isNativeToken,
            currentSdkBalance = currentSdkBalance,
            skipCleanupForMweb = isLitecoinMweb,
        )

        return mwebZeroSnapshotFallbackAvailable ?: adjustedAfterDeduction
    }

    private fun cleanupConfirmedPending(
        relevantPending: List<PendingTransactionEntity>,
        decimals: Int,
        isNativeToken: Boolean,
        currentSdkBalance: BigDecimal,
        skipCleanupForMweb: Boolean,
    ) {
        if (skipCleanupForMweb) {
            // MWEB replaces spent confirmed inputs with unconfirmed change right after broadcast.
            return
        }

        val idsToDelete = mutableListOf<String>()
        val idsToMarkBalanceConfirmed = mutableListOf<String>()

        relevantPending.forEach { entity ->
            if (!entity.isConfirmedByBalance(decimals, isNativeToken, currentSdkBalance)) {
                return@forEach
            }

            if (entity.txHash.isNullOrBlank()) {
                if (entity.blockchainTypeUid == BlockchainType.Ton.uid) {
                    // TON does not return a hash at broadcast, so the row must remain matchable.
                    idsToMarkBalanceConfirmed.add(entity.id)
                }
            } else {
                idsToDelete.add(entity.id)
            }
        }

        if (idsToDelete.isEmpty() && idsToMarkBalanceConfirmed.isEmpty()) return

        scope.launch {
            if (idsToDelete.isNotEmpty()) {
                tryOrNull { pendingRepository.deleteByIds(idsToDelete) }
            }
            if (idsToMarkBalanceConfirmed.isNotEmpty()) {
                tryOrNull { pendingRepository.markBalanceConfirmed(idsToMarkBalanceConfirmed) }
            }
        }
    }

    private fun PendingTransactionEntity.matches(token: Token): Boolean {
        return coinUid == token.coin.uid &&
            tokenTypeId == token.type.id &&
            blockchainTypeUid == token.blockchainType.uid
    }

    private fun PendingTransactionEntity.isConfirmedByBalance(
        decimals: Int,
        isNativeToken: Boolean,
        currentSdkBalance: BigDecimal,
    ): Boolean {
        val amountWithFee = amountWithFee(decimals, isNativeToken)
        val expectedAfterConfirm = sdkBalanceAtCreation(decimals) - amountWithFee
        val tolerance = amountWithFee * CONFIRMATION_BALANCE_TOLERANCE_RATE
        return (currentSdkBalance - expectedAfterConfirm).abs() <= tolerance
    }

    private fun PendingTransactionEntity.amountWithFee(
        decimals: Int,
        isNativeToken: Boolean,
    ): BigDecimal {
        val amount = amountAtomic.toBigDecimal().movePointLeft(decimals)
        val fee = if (isNativeToken) {
            feeAtomic?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }
        return amount + fee
    }

    private fun PendingTransactionEntity.sdkBalanceAtCreation(decimals: Int): BigDecimal {
        return sdkBalanceAtCreationAtomic.toBigDecimal().movePointLeft(decimals)
    }
}
