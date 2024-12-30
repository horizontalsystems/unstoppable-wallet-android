package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.core.ISendEthereumAdapter
import cash.p.terminal.core.managers.EvmKitWrapper
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseEvmAdapter(
    final override val evmKitWrapper: EvmKitWrapper,
    val decimal: Int,
    val coinManager: ICoinManager
) : IAdapter, ISendEthereumAdapter, IBalanceAdapter, IReceiveAdapter {

    val evmKit = evmKitWrapper.evmKit

    override val debugInfo: String
        get() = evmKit.debugInfo()

    val statusInfo: Map<String, Any>
        get() = evmKit.statusInfo()

    // ISendEthereumAdapter

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    // IReceiveAdapter

    override val receiveAddress: String
        get() = evmKit.receiveAddress.eip55

    override val isMainNet: Boolean
        get() = evmKit.chain.isMainNet

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    companion object {
        const val confirmationsThreshold: Int = 12
    }

}
