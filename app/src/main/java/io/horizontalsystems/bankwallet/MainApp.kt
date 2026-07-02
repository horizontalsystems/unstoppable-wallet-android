package io.horizontalsystems.bankwallet

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider

/**
 * Concrete Application for this app. The composition root ([App]) lives in :core;
 * :app only supplies the flavor/BuildConfig-backed configuration.
 */
class MainApp : App() {
    override fun createAppConfigProvider(localStorage: ILocalStorage): IAppConfigProvider =
        AppConfigProvider(localStorage)
}
