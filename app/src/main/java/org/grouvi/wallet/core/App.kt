package org.grouvi.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager

class App : Application() {

    companion object {
        lateinit var preferences: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()

        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }
}