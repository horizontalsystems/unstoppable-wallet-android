package bitcoin.wallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import bitcoin.wallet.core.managers.*
import bitcoin.wallet.core.security.EncryptionManager
import com.squareup.leakcanary.LeakCanary
import io.horizontalsystems.bitcoinkit.WalletKit
import io.horizontalsystems.ethereumkit.EthereumKit
import java.util.*

class App : Application() {

    companion object {

        lateinit var preferences: SharedPreferences

        lateinit var secureStorage: ISecuredStorage
        lateinit var localStorage: ILocalStorage
        lateinit var encryptionManager: EncryptionManager
        lateinit var wordsManager: WordsManager
        lateinit var randomManager: IRandomProvider
        lateinit var networkManager: INetworkManager
        lateinit var currencyManager: ICurrencyManager
        lateinit var exchangeRateManager: IExchangeRateManager
        lateinit var adapterManager: IAdapterManager
        lateinit var backgroundManager: BackgroundManager
        lateinit var languageManager: ILanguageManager


        val testMode = true

        var promptPin = true

        var appBackgroundedTime: Long? = null

        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        // Start WalletKit
        WalletKit.init(this)

        // Initialize EthereumKit
        EthereumKit.init(this)

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val fallbackLanguage = Locale("en")

        backgroundManager = BackgroundManager(this)
        encryptionManager = EncryptionManager()
        secureStorage = SecuredStorageManager(encryptionManager)
        localStorage = LocalStorageManager()
        wordsManager = WordsManager(localStorage, secureStorage)
        randomManager = RandomProvider()
        networkManager = NetworkManager()
        currencyManager = CurrencyManager()
        exchangeRateManager = ExchangeRateManager()
        adapterManager = AdapterManager(wordsManager)
        languageManager = LanguageManager(localStorage, fallbackLanguage)
    }

}
