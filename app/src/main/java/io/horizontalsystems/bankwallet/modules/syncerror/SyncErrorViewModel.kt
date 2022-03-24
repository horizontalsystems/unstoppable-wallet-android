package io.horizontalsystems.bankwallet.modules.syncerror

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.CoinType

class SyncErrorViewModel(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    val reportEmail: String
) : ViewModel() {

    val sourceChangeable = sourceChangeable(wallet.coinType)
    val sourceType = when (wallet.coinType) {
        CoinType.Bitcoin,
        CoinType.BitcoinCash,
        CoinType.Dash,
        CoinType.Litecoin -> SourceType.PrivacySettings
        CoinType.Ethereum,
        is CoinType.Erc20 -> SourceType.EvmNetworkSettings(EvmBlockchain.Ethereum, wallet.account)
        CoinType.BinanceSmartChain,
        is CoinType.Bep20 -> SourceType.EvmNetworkSettings(EvmBlockchain.BinanceSmartChain, wallet.account)
        CoinType.Polygon,
        is CoinType.Mrc20 -> SourceType.EvmNetworkSettings(EvmBlockchain.Polygon, wallet.account)
        CoinType.EthereumOptimism,
        is CoinType.OptimismErc20 -> SourceType.EvmNetworkSettings(EvmBlockchain.Optimism, wallet.account)
        CoinType.EthereumArbitrumOne,
        is CoinType.ArbitrumOneErc20 -> SourceType.EvmNetworkSettings(EvmBlockchain.ArbitrumOne, wallet.account)
        else -> throw IllegalStateException("Source is not changeable")
    }

    fun retry() {
        adapterManager.refreshByWallet(wallet)
    }

    sealed class SourceType {
        object PrivacySettings : SourceType()
        class EvmNetworkSettings(val blockchain: EvmBlockchain, val account: Account) : SourceType()
    }

    private fun sourceChangeable(coinType: CoinType) = when (coinType) {
        is CoinType.Bitcoin,
        is CoinType.BitcoinCash,
        is CoinType.Dash,
        is CoinType.Litecoin,
        is CoinType.BinanceSmartChain,
        is CoinType.Bep20,
        is CoinType.Ethereum,
        is CoinType.Erc20 -> true
        else -> false
    }
}
