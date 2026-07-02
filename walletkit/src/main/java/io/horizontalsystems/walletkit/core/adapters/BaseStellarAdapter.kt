package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendStellarAdapter
import io.horizontalsystems.walletkit.core.managers.StellarKitWrapper

abstract class BaseStellarAdapter(
    stellarKitWrapper: StellarKitWrapper
): IAdapter, IBalanceAdapter, IReceiveAdapter, ISendStellarAdapter {
    protected val stellarKit = stellarKitWrapper.stellarKit
    override val receiveAddress: String = stellarKit.receiveAddress

    override val debugInfo: String
        get() = ""

    // IReceiveAdapter

    override val isMainNet = stellarKit.isMainNet

    override suspend fun send(transactionEnvelope: String) {
        stellarKit.sendTransaction(transactionEnvelope)
    }
}
