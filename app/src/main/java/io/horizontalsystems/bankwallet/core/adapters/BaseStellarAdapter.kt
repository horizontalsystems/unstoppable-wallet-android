package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveStellarAdapter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper

abstract class BaseStellarAdapter(
    stellarKitWrapper: StellarKitWrapper
): IAdapter, IBalanceAdapter, IReceiveStellarAdapter {
    protected val stellarKit = stellarKitWrapper.stellarKit
    override val receiveAddress: String = stellarKit.receiveAddress

    override val debugInfo: String
        get() = ""

    // IReceiveAdapter

    override val isMainNet = stellarKit.isMainNet
}
