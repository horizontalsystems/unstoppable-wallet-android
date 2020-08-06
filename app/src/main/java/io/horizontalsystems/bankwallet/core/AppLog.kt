package io.horizontalsystems.bankwallet.core

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

object AppLog {
    private val file = File(App.instance.filesDir, "app.log").apply {
        createNewFile()
    }

    private val executor = Executors.newSingleThreadExecutor()

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun log(actionId: String, message: String) {
        executor.submit {
            FileOutputStream(file, true).use {
                it.write(String.format("%s: %s: %s", sdf.format(Date()), actionId, message).toByteArray())
            }

            Log.e("AAA", "$actionId: $message")
        }
    }

    fun generateId(prefix: String): String {
        return prefix + "-" + UUID.randomUUID().toString()
    }

    fun getLog(): Map<String, Any> {
        var i = 0
        val res = mutableMapOf<String, String>()

        file.forEachLine {
//            val parts = it.split(": ")
//
//            val date = parts[0]
//            val actionId = parts[1]
//            val message = parts[2]

            res[i++.toString()] = it
        }


        return res
    }
}

