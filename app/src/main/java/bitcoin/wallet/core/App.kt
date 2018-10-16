package bitcoin.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import bitcoin.wallet.core.managers.BackgroundManager
import bitcoin.wallet.kit.WalletKit

class App : Application() {

    companion object {

        lateinit var preferences: SharedPreferences

        val testMode = true

        var promptPin = true

        var appBackgroundedTime: Long? = null

        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Start WalletKit
        WalletKit.init(this)

        BackgroundManager.init(this)

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

}
