package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.reactivex.subjects.PublishSubject

class AuthManager(private val secureStorage: ISecuredStorage,
                  private val localStorage: ILocalStorage,
                  private val coinManager: IWalletManager,
                  private val rateManager: RateManager,
                  private val ethereumKitManager: IEthereumKitManager,
                  private val appConfigProvider: IAppConfigProvider) {

    var adapterManager: IAdapterManager? = null
    var pinManager: IPinManager? = null

    var authData: AuthData? = null
    var authDataSignal = PublishSubject.create<Unit>()

    @Throws(UserNotAuthenticatedException::class)
    fun safeLoad() {
        authData = secureStorage.authData
        authDataSignal.onNext(Unit)
    }

    @Throws(UserNotAuthenticatedException::class)
    fun login(words: List<String>, syncMode: SyncMode) {
        AuthData(words).let {
            secureStorage.saveAuthData(it)
            localStorage.syncMode = syncMode
            authData = it
            // coinManager.enableDefaultWallets()
        }
    }

    fun logout() {
        adapterManager?.stopKits()

        EthereumAdapter.clear(App.instance)
        Erc20Adapter.clear(App.instance)

        authData?.let { authData ->
            BitcoinAdapter.clear(App.instance, authData.walletId, appConfigProvider.testMode)
            BitcoinCashAdapter.clear(App.instance, authData.walletId, appConfigProvider.testMode)
            DashAdapter.clear(App.instance, authData.walletId, appConfigProvider.testMode)
        }

        pinManager?.clear()
        localStorage.clear()
        coinManager.clear()
        rateManager.clear()

//        todo: clear authData from secureStorage. note clearing localstorage also clears auth data
//        secureStorage.clearAuthData()
        authData = null

    }

}
