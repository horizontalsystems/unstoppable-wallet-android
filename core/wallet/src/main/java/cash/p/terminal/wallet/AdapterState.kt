package cash.p.terminal.wallet

import java.util.Date

sealed class AdapterState {
    object Synced : AdapterState()
    data class Syncing(val progress: Int? = null, val lastBlockDate: Date? = null) : AdapterState()
    data class SearchingTxs(val count: Int) : AdapterState()
    data class NotSynced(val error: Throwable) : AdapterState()

    override fun toString(): String {
        return when (this) {
            is Synced -> "Synced"
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""} lastBlockDate: $lastBlockDate"
            is SearchingTxs -> "SearchingTxs count: $count"
            is NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
        }
    }
}