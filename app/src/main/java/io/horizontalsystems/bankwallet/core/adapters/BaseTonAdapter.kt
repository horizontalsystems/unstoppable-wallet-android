package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.core.managers.statusInfo
import io.horizontalsystems.tonkit.models.Network

abstract class BaseTonAdapter(
    tonKitWrapper: TonKitWrapper,
    val decimals: Int
) : IAdapter, IBalanceAdapter, IReceiveAdapter {

    val tonKit = tonKitWrapper.tonKit

    override val debugInfo: String
        get() = ""

    val statusInfo: Map<String, Any>
        get() = tonKit.statusInfo()

    // IReceiveAdapter

    override val receiveAddress: String
        get() = tonKit.receiveAddress.toUserFriendly(false)

    override val isMainNet: Boolean
        get() = tonKit.network == Network.MainNet

    // ISendTronAdapter
}
