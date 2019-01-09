package io.horizontalsystems.bankwallet.core

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.core.factories.WalletFactory
import io.horizontalsystems.bankwallet.core.managers.*
import io.horizontalsystems.bankwallet.core.security.EncryptionManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.core.storage.RatesRepository
import io.horizontalsystems.bankwallet.core.storage.TransactionRepository
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoFactory
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.EthereumKit
import java.util.*

class App : Application() {

    companion object {

        lateinit var preferences: SharedPreferences

        lateinit var secureStorage: ISecuredStorage
        lateinit var localStorage: ILocalStorage
        lateinit var encryptionManager: EncryptionManager
        lateinit var wordsManager: WordsManager
        lateinit var authManager: AuthManager
        lateinit var randomManager: IRandomProvider
        lateinit var networkManager: INetworkManager
        lateinit var currencyManager: ICurrencyManager
        lateinit var backgroundManager: BackgroundManager
        lateinit var languageManager: ILanguageManager
        lateinit var systemInfoManager: ISystemInfoManager
        lateinit var pinManager: IPinManager
        lateinit var lockManager: ILockManager
        lateinit var appConfigProvider: IAppConfigProvider
        lateinit var walletManager: IWalletManager
        lateinit var coinManager: CoinManager

        lateinit var rateSyncer: RateSyncer
        lateinit var rateManager: RateManager
        lateinit var networkAvailabilityManager: NetworkAvailabilityManager
        lateinit var transactionRateSyncer: ITransactionRateSyncer
        lateinit var transactionManager: TransactionManager
        lateinit var appDatabase: AppDatabase
        lateinit var transactionStorage: ITransactionRecordStorage
        lateinit var rateStorage: IRateStorage
        lateinit var transactionInfoFactory: FullTransactionInfoFactory
        lateinit var appCloseManager: AppCloseManager

        val testMode = true

        lateinit var instance: App
            private set

        var lastExitDate: Long = 0
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        // Initialize BitcoinKit
        BitcoinKit.init(this)

        // Initialize EthereumKit
        EthereumKit.init(this)

        instance = this
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val fallbackLanguage = Locale("en")

        backgroundManager = BackgroundManager(this)
        encryptionManager = EncryptionManager()
        secureStorage = SecuredStorageManager(encryptionManager)
        localStorage = LocalStorageManager()
        wordsManager = WordsManager(localStorage)
        authManager = AuthManager(secureStorage, localStorage)
        randomManager = RandomProvider()
        networkManager = NetworkManager()
        systemInfoManager = SystemInfoManager()
        pinManager = PinManager(secureStorage)
        lockManager = LockManager(secureStorage, authManager)
        appConfigProvider = AppConfigProvider()
        languageManager = LanguageManager(localStorage, appConfigProvider, fallbackLanguage)
        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        coinManager = CoinManager(appConfigProvider)
        walletManager = WalletManager(coinManager, authManager, WalletFactory(AdapterFactory(appConfigProvider)))

        networkAvailabilityManager = NetworkAvailabilityManager()

        appDatabase = AppDatabase.getInstance(this)
        transactionStorage = TransactionRepository(appDatabase)

        appCloseManager = AppCloseManager()

        rateStorage = RatesRepository(appDatabase)
        rateManager = RateManager(rateStorage, networkManager)
        rateSyncer = RateSyncer(rateManager, walletManager, currencyManager, networkAvailabilityManager)

        transactionRateSyncer = TransactionRateSyncer(transactionStorage, networkManager)
        transactionManager = TransactionManager(transactionStorage, transactionRateSyncer, walletManager, currencyManager, wordsManager, networkAvailabilityManager)
        transactionInfoFactory = FullTransactionInfoFactory(networkManager, appConfigProvider)

        authManager.walletManager = walletManager
        authManager.pinManager = pinManager
        authManager.transactionManager = transactionManager
    }

}
