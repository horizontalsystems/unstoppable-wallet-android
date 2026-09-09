package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendXrpAdapter
import io.horizontalsystems.walletkit.core.managers.XrpKitWrapper
import java.math.BigDecimal

/** Shared kit access for the XRP and issued-token adapters of one account. */
abstract class BaseXrpAdapter(
    xrpKitWrapper: XrpKitWrapper,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendXrpAdapter {
    val kit: XrpKit = xrpKitWrapper.kit

    override val receiveAddress: String = kit.receiveAddress
    override val isMainNet: Boolean = kit.isMainNet
    override val debugInfo: String get() = ""

    override val isAccountActivated: Boolean get() = kit.isAccountActivated

    override fun validate(address: String) {
        XrpKit.validateAddress(address)
    }

    override suspend fun requiresDestinationTag(address: String): Boolean {
        return kit.requiresDestinationTag(classicAddress(address))
    }

    protected fun classicAddress(address: String): String =
        XrpKit.decodeXAddress(address)?.first ?: address

    companion object {
        /** Used until the network reports a fee (base fee is 10 drops; 12 leaves headroom). */
        val DEFAULT_FEE: BigDecimal = BigDecimal("0.000012")
    }
}
