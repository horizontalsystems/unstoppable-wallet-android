package io.horizontalsystems.bankwallet.core.managers

import android.security.keystore.UserNotAuthenticatedException
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AuthData
import io.reactivex.subjects.PublishSubject

class AuthManager(private val secureStorage: ISecuredStorage,
                  private val localStorage: ILocalStorage,
                  private val coinManager: ICoinManager,
                  private val rateManager: RateManager,
                  private val ethereumKitManager: IEthereumKitManager,
                  private val appConfigProvider: IAppConfigProvider) {

    var adapterManager: IAdapterManager? = null
    var pinManager: IPinManager? = null

    var authData: AuthData? = null
    var authDataSignal = PublishSubject.create<Unit>()

    var isLoggedIn: Boolean = false
        get() = !secureStorage.noAuthData()


    @Throws(UserNotAuthenticatedException::class)
    fun safeLoad() {
        authData = secureStorage.authData
        authDataSignal.onNext(Unit)
    }

    @Throws(UserNotAuthenticatedException::class)
    fun login(words: List<String>, newWallet: Boolean) {
        AuthData(words).let {
            secureStorage.saveAuthData(it)
            localStorage.isNewWallet = newWallet
            authData = it
            coinManager.enableDefaultCoins()
            adapterManager?.initAdapters()
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
