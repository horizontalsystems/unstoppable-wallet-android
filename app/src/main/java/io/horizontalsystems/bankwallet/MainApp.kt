package io.horizontalsystems.bankwallet

import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.walletkit.chain.evm.EvmChainPlugin
import io.horizontalsystems.walletkit.chain.tron.TronChainPlugin
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.chain.bitcoin.BitcoinCashChainPlugin
import io.horizontalsystems.walletkit.chain.bitcoin.BitcoinChainPlugin
import io.horizontalsystems.walletkit.chain.bitcoin.DashChainPlugin
import io.horizontalsystems.walletkit.chain.bitcoin.ECashChainPlugin
import io.horizontalsystems.walletkit.chain.bitcoin.LitecoinChainPlugin
import io.horizontalsystems.walletkit.chain.monero.MoneroChainPlugin
import io.horizontalsystems.walletkit.chain.zano.ZanoChainPlugin
import io.horizontalsystems.walletkit.chain.solana.SolanaChainPlugin
import io.horizontalsystems.walletkit.chain.stellar.StellarChainPlugin
import io.horizontalsystems.walletkit.chain.thorchain.MayachainChainPlugin
import io.horizontalsystems.walletkit.chain.thorchain.ThorchainChainPlugin
import io.horizontalsystems.walletkit.chain.ton.TonChainPlugin
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
        // EVM chains come first so BlockchainType.supported keeps its pre-plugin order.
        EvmBlockchainManager.blockchainTypes.forEach { blockchainType ->
            ChainRegistry.register(EvmChainPlugin(blockchainType))
        }
        // Tron right after the EVM chains keeps its pre-plugin position in BlockchainType.supported.
        ChainRegistry.register(TronChainPlugin())
        ChainRegistry.register(BitcoinChainPlugin())
        ChainRegistry.register(BitcoinCashChainPlugin())
        ChainRegistry.register(ECashChainPlugin())
        ChainRegistry.register(LitecoinChainPlugin())
        ChainRegistry.register(DashChainPlugin())
        // Registration order defines the tail of BlockchainType.supported:
        // bitcoin, bitcoinCash, ecash, litecoin, dash, monero, zano, zcash, solana, stellar, ton.
        ChainRegistry.register(MoneroChainPlugin({ App.instance }, { App.moneroNodeManager }))
        ChainRegistry.register(ZanoChainPlugin({ App.zanoNodeManager }, { App.backgroundManager }))
        ChainRegistry.register(ZcashChainPlugin({ App.instance }, { App.zcashEndpointManager }, { App.localStorage }))
        ChainRegistry.register(SolanaChainPlugin({ BuildConfig.SOLANA_ALCHEMY_API_KEY }, { BuildConfig.SOLANA_JUPITER_API_KEY }))
        ChainRegistry.register(StellarChainPlugin())
        ChainRegistry.register(TonChainPlugin())
        ChainRegistry.register(ThorchainChainPlugin())
        ChainRegistry.register(MayachainChainPlugin())
    }
}
