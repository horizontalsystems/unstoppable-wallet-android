package io.horizontalsystems.pin.core

import android.content.Context
import android.content.SharedPreferences
import io.horizontalsystems.core.helpers.DateHelper
import java.util.*

class LockManager(private val pinManager: PinManager, private val context: Context) {

    var isLocked: Boolean = false
    private val lockTimeout = 60L
    private var lastExitDate: Long
        get() {
            return getPreferences(context).getLong(BackgroundedTime, 0)
        }
        set(value) {
            getPreferences(context)
                    .edit()
                    .putLong(BackgroundedTime, value)
                    .commit()
        }

    fun didEnterBackground() {
        if (isLocked) {
            return
        }

        lastExitDate = Date().time
    }

    fun willEnterForeground() {
        if (isLocked || !pinManager.isPinSet) {
            return
        }

        val secondsAgo = DateHelper.getSecondsAgo(lastExitDate)
        if (secondsAgo > lockTimeout) {
            isLocked = true
        }
    }

    fun onUnlock() {
        isLocked = false
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(LockManager::class.java.name, Context.MODE_PRIVATE)
    }

    fun updateLastExitDate() {
        lastExitDate = Date().time
    }

    companion object{
        private const val BackgroundedTime = "LockManager.BackgroundedTime"
    }

}
