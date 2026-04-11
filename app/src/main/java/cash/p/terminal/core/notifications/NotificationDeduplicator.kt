package cash.p.terminal.core.notifications

import android.content.SharedPreferences
import androidx.core.content.edit

class NotificationDeduplicator(
    private val preferences: SharedPreferences,
) {
    private val notifiedUids = mutableSetOf<String>()
    private val lastCheckTimes = mutableMapOf<String, Long>()
    private val lastCheckUids = mutableMapOf<String, MutableSet<String>>()

    init {
        loadLastCheckTimes()
    }

    fun isNew(recordUid: String, blockchainUid: String, timestamp: Long): Boolean {
        if (recordUid in notifiedUids) return false
        val lastCheck = lastCheckTimes[blockchainUid] ?: return false
        return when {
            timestamp > lastCheck -> true
            timestamp < lastCheck -> false
            else -> recordUid !in (lastCheckUids[blockchainUid] ?: return false)
        }
    }

    fun markNotified(recordUid: String) {
        notifiedUids.add(recordUid)
    }

    fun updateLastCheckTime(
        blockchainUid: String,
        timestamp: Long,
        recordUidsAtTimestamp: Set<String> = emptySet(),
    ) {
        val current = lastCheckTimes[blockchainUid] ?: 0L

        when {
            timestamp < current -> return
            timestamp > current -> {
                lastCheckTimes[blockchainUid] = timestamp
                updateLastCheckUids(blockchainUid, recordUidsAtTimestamp)
                preferences.edit {
                    putLong(timeKey(blockchainUid), timestamp)
                    persistLastCheckUids(this, blockchainUid)
                }
            }
            recordUidsAtTimestamp.isNotEmpty() -> {
                lastCheckUids.getOrPut(blockchainUid) { mutableSetOf() }
                    .addAll(recordUidsAtTimestamp)
                preferences.edit {
                    persistLastCheckUids(this, blockchainUid)
                }
            }
        }
    }

    fun reset() {
        notifiedUids.clear()
    }

    private fun loadLastCheckTimes() {
        preferences.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX_TIME) && value is Long) {
                val uid = key.removePrefix(KEY_PREFIX_TIME)
                lastCheckTimes[uid] = value
            } else if (key.startsWith(KEY_PREFIX_UIDS) && value is Set<*>) {
                val uid = key.removePrefix(KEY_PREFIX_UIDS)
                val uids = value.filterIsInstance<String>()
                if (uids.isNotEmpty()) {
                    lastCheckUids[uid] = uids.toMutableSet()
                }
            }
        }
    }

    private fun updateLastCheckUids(
        blockchainUid: String,
        recordUidsAtTimestamp: Set<String>,
    ) {
        if (recordUidsAtTimestamp.isEmpty()) {
            lastCheckUids.remove(blockchainUid)
        } else {
            lastCheckUids[blockchainUid] = recordUidsAtTimestamp.toMutableSet()
        }
    }

    private fun persistLastCheckUids(
        editor: SharedPreferences.Editor,
        blockchainUid: String,
    ) {
        val uids = lastCheckUids[blockchainUid]
        if (uids.isNullOrEmpty()) {
            editor.remove(uidKey(blockchainUid))
        } else {
            editor.putStringSet(uidKey(blockchainUid), uids)
        }
    }

    companion object {
        private const val KEY_PREFIX_TIME = "push_last_check_"
        private const val KEY_PREFIX_UIDS = "push_last_check_uids_"

        private fun timeKey(blockchainUid: String) = "$KEY_PREFIX_TIME$blockchainUid"
        private fun uidKey(blockchainUid: String) = "$KEY_PREFIX_UIDS$blockchainUid"
    }
}
