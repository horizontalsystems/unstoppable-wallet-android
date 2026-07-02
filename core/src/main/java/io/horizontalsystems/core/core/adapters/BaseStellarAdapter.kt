package io.horizontalsystems.core.core.adapters

import io.horizontalsystems.core.core.IAdapter
import io.horizontalsystems.core.core.IBalanceAdapter
import io.horizontalsystems.core.core.IReceiveAdapter
import io.horizontalsystems.core.core.ISendStellarAdapter
import io.horizontalsystems.core.core.managers.StellarKitWrapper

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
