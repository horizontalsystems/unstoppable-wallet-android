package io.horizontalsystems.bankwallet.core.adapters

import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.core.managers.statusInfo
import io.horizontalsystems.tonkit.models.Network
import java.math.BigDecimal

abstract class BaseTonAdapter(
    tonKitWrapper: TonKitWrapper,
    val decimals: Int
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendTonAdapter {

    protected val tonKit = tonKitWrapper.tonKit

    override val debugInfo: String
        get() = ""

    val statusInfo: Map<String, Any>
        get() = tonKit.statusInfo()

    // IReceiveAdapter

    override val receiveAddress: String
        get() = tonKit.receiveAddress.toUserFriendly(false)

    override val isMainNet: Boolean
        get() = tonKit.network == Network.MainNet

    final override suspend fun sign(request: SendRequestEntity) = tonKit.sign(request)

    final override suspend fun send(boc: String) = tonKit.send(boc)

    final override suspend fun estimateFee(boc: String): BigDecimal {
        val estimateFee = tonKit.estimateFee(boc)
        return estimateFee.toBigDecimal(9).stripTrailingZeros()
    }
}
