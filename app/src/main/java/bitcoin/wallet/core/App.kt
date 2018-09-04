package bitcoin.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import bitcoin.wallet.core.managers.BackgroundManager
import bitcoin.wallet.injections.component.AppComponent
import bitcoin.wallet.injections.component.DaggerAppComponent
import bitcoin.wallet.injections.module.AppModule
import bitcoin.wallet.kit.WalletKit
import io.realm.Realm
import javax.inject.Inject

class App : Application() {

    @Inject
    lateinit var exchangeRateService: ExchangeRateService

    companion object {

        lateinit var appComponent: AppComponent
        lateinit var preferences: SharedPreferences

        val testMode = true

        var promptPin = true

        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)

        // Start WalletKit
        WalletKit.init(this)

        appComponent = DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()

        appComponent.inject(this)


        BackgroundManager.init(this)

        startBlockchainService()

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    private fun startBlockchainService() {
        exchangeRateService.start()
    }

}
