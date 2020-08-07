package io.horizontalsystems.bankwallet.core

import android.util.Log
import io.horizontalsystems.bankwallet.core.storage.LogsDao
import io.horizontalsystems.bankwallet.entities.LogEntry
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

object AppLog {
    lateinit var logsDao: LogsDao

    private val executor = Executors.newSingleThreadExecutor()

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun info(actionId: String, message: String) {
        executor.submit {
            logsDao.insert(LogEntry(System.currentTimeMillis(), Log.INFO, actionId, message))
        }
    }

    fun generateId(prefix: String): String {
        return prefix + ":" + UUID.randomUUID().toString()
    }

    fun getLog(): Map<String, Any> {
        val res = mutableMapOf<String, MutableMap<String, String>>()

        logsDao.getAll().forEach { logEntry ->
            if (!res.containsKey(logEntry.actionId)) {
                res[logEntry.actionId] = mutableMapOf()
            }

            val logMessage = sdf.format(Date(logEntry.date)) + " " + logEntry.message

            res[logEntry.actionId]?.set(logEntry.id.toString(), logMessage)
        }

        return res
    }
}

