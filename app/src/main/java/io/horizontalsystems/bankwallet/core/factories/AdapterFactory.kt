package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.EthereumAdapter
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(private val appConfigProvider: IAppConfigProvider) {

    fun adapterForCoin(coinType: CoinType, authData: AuthData, newWallet: Boolean) = when (coinType) {
            is CoinType.Bitcoin -> BitcoinAdapter.createBitcoin(authData.words, appConfigProvider.testMode, newWallet, authData.walletId)
            is CoinType.BitcoinCash -> BitcoinAdapter.createBitcoinCash(authData.words, appConfigProvider.testMode, newWallet, authData.walletId)
            is CoinType.Ethereum -> EthereumAdapter.createEthereum(authData.words, appConfigProvider.testMode)
            is CoinType.Erc20 -> null
    }

}
