package bitcoin.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.core.managers.BackgroundManager
import bitcoin.wallet.core.managers.Factory
import io.realm.Realm

class App : Application() {

    companion object {
        lateinit var preferences: SharedPreferences

        val testMode = true

        var promptPin = true

        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        Realm.init(this)

        BackgroundManager.init(this)

        startBlockchainService()

        instance = this
    }

    private fun startBlockchainService() {
        // todo: implement Blockchain as service
        BlockchainManager.localStorage = Factory.preferencesManager
        BlockchainManager.init(filesDir, resources.assets, BlockchainStorage, testMode)

        ExchangeRateService.networkManager = Factory.networkManager
        ExchangeRateService.start(BlockchainStorage)
    }

}
