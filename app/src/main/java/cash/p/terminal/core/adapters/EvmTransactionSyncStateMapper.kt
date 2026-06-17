package cash.p.terminal.core.adapters

import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.wallet.AdapterState
import io.horizontalsystems.ethereumkit.core.EthereumKit

internal val EvmTransactionRepository.transactionHistoryAdapterState: AdapterState
    get() = forwardSyncingState() ?: transactionSyncErrorOrSynced()

private fun EvmTransactionRepository.forwardSyncingState(): AdapterState.Syncing? {
    val state = forwardSyncState.value as? EthereumKit.ForwardSyncState.Syncing ?: return null
    return AdapterState.Syncing(progress = 0.0, blocksRemained = state.blocksRemaining)
}

// Background polling cycle (BSC re-runs TransactionSyncManager every ~15s on each new
// block height) is intentionally suppressed: only error states are surfaced. Otherwise
// the UI would flash a parameter-less spinner on every poll.
private fun EvmTransactionRepository.transactionSyncErrorOrSynced(): AdapterState =
    when (val txSync = transactionsSyncState) {
        is EthereumKit.SyncState.NotSynced ->
            if (txSync.error is EthereumKit.SyncError.NotStarted) {
                AdapterState.Synced
            } else {
                AdapterState.NotSynced(txSync.error)
            }
        else -> AdapterState.Synced
    }
