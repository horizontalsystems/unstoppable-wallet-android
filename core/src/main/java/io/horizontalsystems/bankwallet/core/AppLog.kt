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
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun info(actionId: String, message: String) {
        executor.submit {
            logsDao.insert(LogEntry(System.currentTimeMillis(), Log.INFO, actionId, message))
        }
    }

    fun warning(actionId: String, message: String, e: Throwable) {
        executor.submit {
            logsDao.insert(LogEntry(System.currentTimeMillis(), Log.WARN, actionId, message + ": " + getStackTraceString(e)))
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

    private fun getStackTraceString(error: Throwable): String {
        val sb = StringBuilder()

        sb.appendLine(error)

        error.stackTrace.forEachIndexed { index, stackTraceElement ->
            if (index < 5) {
                sb.appendLine(stackTraceElement)
            }
        }

        return sb.toString()
    }
}

