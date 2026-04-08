package cash.p.terminal.core.notifications

import android.content.SharedPreferences
import androidx.core.content.edit

class NotificationDeduplicator(
    private val preferences: SharedPreferences,
) {
    private val notifiedUids = mutableSetOf<String>()
    private val lastCheckTimes = mutableMapOf<String, Long>()

    init {
        loadLastCheckTimes()
    }

    fun isNew(recordUid: String, blockchainUid: String, timestamp: Long): Boolean {
        if (recordUid in notifiedUids) return false
        val lastCheck = lastCheckTimes[blockchainUid] ?: return false
        return timestamp > lastCheck
    }

    fun markNotified(recordUid: String) {
        notifiedUids.add(recordUid)
    }

    fun updateLastCheckTime(blockchainUid: String, timestamp: Long) {
        val current = lastCheckTimes[blockchainUid] ?: 0L
        if (timestamp <= current) return
        lastCheckTimes[blockchainUid] = timestamp
        preferences.edit {
            putLong("$KEY_PREFIX$blockchainUid", timestamp)
        }
    }

    fun reset() {
        notifiedUids.clear()
    }

    private fun loadLastCheckTimes() {
        preferences.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is Long) {
                val uid = key.removePrefix(KEY_PREFIX)
                lastCheckTimes[uid] = value
            }
        }
    }

    companion object {
        private const val KEY_PREFIX = "push_last_check_"
    }
}
