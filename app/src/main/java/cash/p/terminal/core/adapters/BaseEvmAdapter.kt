package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.data.repository.EvmTransactionRepository
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import timber.log.Timber
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

    // ISendEthereumAdapter

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    // IReceiveAdapter

    override val receiveAddress: String
        get() = evmTransactionRepository.receiveAddress.eip55

    override val isMainNet: Boolean
        get() = evmTransactionRepository.chain.isMainNet

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    protected fun historicalSyncAdapterState(): AdapterState? {
        if (evmTransactionRepository.getBlockchainType() != BlockchainType.BinanceSmartChain) return null
        val histState = evmTransactionRepository.historicalSyncState.value
        Timber.d("BaseEvmAdapter historicalSyncState: $histState")
        if (histState is EthereumKit.HistoricalSyncState.Syncing) {
            Timber.d("BaseEvmAdapter progress: ${histState.progress}, blocks remaining: ${histState.blocksRemaining}")
            return AdapterState.Syncing(
                progress = (histState.progress * 100).toInt(),
                blocksRemained = histState.blocksRemaining
            )
        }
        return null
    }

    protected fun forwardSyncAdapterState(): AdapterState? {
        val fwdState = evmTransactionRepository.forwardSyncState.value
        if (fwdState is EthereumKit.ForwardSyncState.Syncing) {
            return AdapterState.Syncing(
                progress = 0,
                blocksRemained = fwdState.blocksRemaining
            )
        }
        return null
    }

    companion object {
        const val confirmationsThreshold: Int = 12
    }

}
