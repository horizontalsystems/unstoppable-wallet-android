package io.horizontalsystems.bankwallet.modules.syncerror

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
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
        is CoinType.Erc20 -> SourceType.EvmNetworkSettings(EvmNetworkModule.Blockchain.Ethereum, wallet.account)
        CoinType.BinanceSmartChain,
        is CoinType.Bep20 -> SourceType.EvmNetworkSettings(EvmNetworkModule.Blockchain.BinanceSmartChain, wallet.account)
        else -> throw IllegalStateException("Source is not changeable")
    }

    fun retry() {
        adapterManager.refreshByWallet(wallet)
    }

    sealed class SourceType {
        object PrivacySettings : SourceType()
        class EvmNetworkSettings(val blockchain: EvmNetworkModule.Blockchain, val account: Account) : SourceType()
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
