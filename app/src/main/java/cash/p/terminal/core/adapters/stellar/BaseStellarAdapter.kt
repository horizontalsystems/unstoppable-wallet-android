package cash.p.terminal.core.adapters.stellar

import cash.p.terminal.core.managers.StellarKitWrapper
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter

abstract class BaseStellarAdapter(
    stellarKitWrapper: StellarKitWrapper
): IAdapter, IBalanceAdapter, IReceiveAdapter {
    protected val stellarKit = stellarKitWrapper.stellarKit
    override val receiveAddress: String = stellarKit.receiveAddress

    override val debugInfo: String
        get() = ""

    // IReceiveAdapter

    override val isMainNet = stellarKit.isMainNet
}
