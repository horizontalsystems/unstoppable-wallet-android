package cash.p.terminal.core.adapters

import cash.p.terminal.core.IAdapter
import cash.p.terminal.core.IBalanceAdapter
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.managers.TronKitWrapper
import io.horizontalsystems.tronkit.transaction.Signer
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseTronAdapter(
    tronKitWrapper: TronKitWrapper,
    val decimal: Int
) : IAdapter, IBalanceAdapter, IReceiveAdapter {

    val tronKit = tronKitWrapper.tronKit
    protected val signer: Signer? = tronKitWrapper.signer

    override val debugInfo: String
        get() = ""

    // IReceiveAdapter

    override val receiveAddress: String
        get() = tronKit.address.base58

    protected fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
    }

    protected fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    protected fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    companion object {
        const val confirmationsThreshold: Int = 19
    }

}
