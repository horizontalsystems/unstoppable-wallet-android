package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.view.inputmethod.InputMethodManager


object Utils {

    fun isUsingCustomKeyboard(context: Context): Boolean {

        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val inputMethodProperties = inputMethodManager.enabledInputMethodList
        for (i in 0 until inputMethodProperties.size) {
            val imi = inputMethodProperties[i]
            if (imi.id == Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)) {
                if (imi.serviceInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                    return true
                }
            }
        }

        return false
    }
}
