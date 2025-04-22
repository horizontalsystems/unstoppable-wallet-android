package io.horizontalsystems.stellarkit

sealed class SyncState(val description: String) {
    object Synced : SyncState("Synced")
    object Syncing : SyncState("Syncing")
    data class NotSynced(val error: Throwable) : SyncState("Not Synced: ${error.message}")

    override fun equals(other: Any?): Boolean {
        if (this is Synced && other is Synced) {
            return true
        }
        if (this is Syncing && other is Syncing) {
            return true
        }
        if (this is NotSynced && other is NotSynced) {
            return this.error.equals(other.error)
        }
        return false
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
