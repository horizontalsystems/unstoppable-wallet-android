package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.managers.StellarKitWrapper
import io.horizontalsystems.bankwallet.core.managers.statusInfo

abstract class BaseStellarAdapter(
    stellarKitWrapper: StellarKitWrapper
): IAdapter, IBalanceAdapter, IReceiveAdapter {
    protected val stellarKit = stellarKitWrapper.stellarKit
    override val receiveAddress: String = stellarKit.receiveAddress

    override val debugInfo: String
        get() = ""

    val statusInfo: Map<String, Any>
        get() = stellarKit.statusInfo()

    // IReceiveAdapter

    override val isMainNet = stellarKit.isMainNet
}
