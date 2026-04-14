package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.IAdapter
import com.quantum.wallet.bankwallet.core.IBalanceAdapter
import com.quantum.wallet.bankwallet.core.IReceiveAdapter
import com.quantum.wallet.bankwallet.core.ISendStellarAdapter
import com.quantum.wallet.bankwallet.core.managers.StellarKitWrapper

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
