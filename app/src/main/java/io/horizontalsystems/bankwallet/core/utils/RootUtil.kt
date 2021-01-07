package io.horizontalsystems.bankwallet.core.utils

import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootUtil {

    fun isRooted(): Boolean {
        return buildTags() || checkPaths() || checkSu()
    }

    private fun buildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkPaths(): Boolean {
        arrayOf(
                "/data/local/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/sbin/su",
                "/system/app/Superuser.apk",
                "/system/bin/failsafe/su",
                "/system/bin/su",
                "/system/sd/xbin/su",
                "/system/xbin/su"
        ).forEach {
            if (File(it).exists())
                return true
        }

        return false
    }

    private fun checkSu(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val input = BufferedReader(InputStreamReader(process.inputStream))
            input.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
}
