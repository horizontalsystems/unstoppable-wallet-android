package bitcoin.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric



class App : Application() {

    companion object {
        lateinit var preferences: SharedPreferences

        val testMode = true
    }

    override fun onCreate() {
        super.onCreate()

        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        Fabric.with(this, Crashlytics())
    }
}