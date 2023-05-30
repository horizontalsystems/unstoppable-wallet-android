package cash.p.terminal.core.adapters

import cash.p.terminal.core.IAdapter
import cash.p.terminal.core.IBalanceAdapter
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.managers.TronKitWrapper
import io.horizontalsystems.tronkit.transaction.Signer

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

    companion object {
        const val confirmationsThreshold: Int = 19
    }

}
