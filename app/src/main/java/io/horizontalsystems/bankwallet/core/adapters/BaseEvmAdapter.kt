package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseEvmAdapter(
    final override val evmKitWrapper: EvmKitWrapper,
    val decimal: Int,
    val coinManager: ICoinManager
) : IAdapter, ISendEthereumAdapter, IBalanceAdapter, IReceiveAdapter {

    val evmKit = evmKitWrapper.evmKit

    override val isMainnet: Boolean
        get() = evmKit.chain.isMainNet

    override val debugInfo: String
        get() = evmKit.debugInfo()

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

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    companion object {
        const val confirmationsThreshold: Int = 12
    }

}
