package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal
import java.math.BigInteger

internal abstract class BaseEvmAdapter(
    final override val evmTransactionRepository: EvmTransactionRepository,
    val decimal: Int,
    val coinManager: ICoinManager
) : IAdapter, ISendEthereumAdapter, IBalanceAdapter, IReceiveAdapter {

    override val debugInfo: String
        get() = evmTransactionRepository.debugInfo()

    override val statusInfo: Map<String, Any>
        get() = evmTransactionRepository.statusInfo()

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    override val receiveAddress: String
        get() = evmTransactionRepository.receiveAddress.eip55

    override val isMainNet: Boolean
        get() = evmTransactionRepository.chain.isMainNet

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    // Decoupled from balance readiness: drives the spinner, must not block send/swap.
    // Historical sync intentionally excluded: its blocksRemaining starts from chain tip (~89.7M on BSC)
    // and would render as a misleading "89.7M blocks remaining" message in the UI.
    override val transactionsSyncState: AdapterState
        get() = evmTransactionRepository.transactionHistoryAdapterState

    override val transactionsSyncStateUpdatedFlow: Flow<Unit>
        get() = merge(
            evmTransactionRepository.transactionsSyncStateFlowable.map { }.asFlow(),
            evmTransactionRepository.forwardSyncState.map { },
        )

    companion object {
        const val confirmationsThreshold: Int = 12
    }
}
