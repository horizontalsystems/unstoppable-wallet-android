package cash.p.terminal.core.adapters

import cash.p.terminal.core.IAdapter
import cash.p.terminal.core.IBalanceAdapter
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.managers.SolanaKitWrapper
import io.horizontalsystems.solanakit.Signer

abstract class BaseSolanaAdapter(
        solanaKitWrapper: SolanaKitWrapper,
        val decimal: Int
) : IAdapter, IBalanceAdapter, IReceiveAdapter {

    val solanaKit = solanaKitWrapper.solanaKit
    protected val signer: Signer? = solanaKitWrapper.signer

    override val isMainnet: Boolean
        get() = solanaKit.isMainnet

    override val debugInfo: String
        get() = solanaKit.debugInfo()

    // IReceiveAdapter

    override val receiveAddress: String
        get() = solanaKit.receiveAddress

    companion object {
        const val confirmationsThreshold: Int = 12
    }

}
