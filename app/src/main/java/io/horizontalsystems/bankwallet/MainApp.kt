package io.horizontalsystems.bankwallet

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.walletkit.chain.monero.MoneroChainPlugin
import io.horizontalsystems.walletkit.chain.zano.ZanoChainPlugin
import io.horizontalsystems.walletkit.chain.zcash.ZcashChainPlugin
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider

/**
 * Concrete Application for this app. The composition root ([App]) lives in :core;
 * :app only supplies the flavor/BuildConfig-backed configuration.
 */
class MainApp : App() {
    override fun createAppConfigProvider(localStorage: ILocalStorage): IAppConfigProvider =
        AppConfigProvider(localStorage)

    override fun registerChainPlugins() {
        // Registration order defines the tail of BlockchainType.supported: monero, zano.
        ChainRegistry.register(MoneroChainPlugin({ App.instance }, { App.moneroNodeManager }))
        ChainRegistry.register(ZanoChainPlugin({ App.zanoNodeManager }, { App.backgroundManager }))
        ChainRegistry.register(ZcashChainPlugin({ App.instance }, { App.zcashEndpointManager }, { App.localStorage }))
    }
}
