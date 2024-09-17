package cash.p.terminal.core.adapters

import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.IAdapter
import cash.p.terminal.core.IBalanceAdapter
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.core.managers.network
import cash.p.terminal.core.managers.statusInfo
import io.horizontalsystems.tonkit.models.Network
import io.horizontalsystems.tonkit.models.SyncState

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
        get() = tonKit.receiveAddress.toUserFriendly()

    override val isMainNet: Boolean
        get() = tonKit.network == Network.MainNet

    // ISendTronAdapter

    protected fun convertToAdapterState(syncState: SyncState) = when (syncState) {
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.Syncing -> AdapterState.Syncing()
    }
}
