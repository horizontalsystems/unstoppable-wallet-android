package io.horizontalsystems.bankwallet

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.IAppConfigProvider

/**
 * Concrete Application for this app. The composition root ([App]) lives in :core;
 * :app only supplies the flavor/BuildConfig-backed configuration.
 */
class MainApp : App() {
    override fun createAppConfigProvider(localStorage: ILocalStorage): IAppConfigProvider =
        AppConfigProvider(localStorage)
}
