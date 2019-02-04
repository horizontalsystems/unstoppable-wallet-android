package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(private val appConfigProvider: IAppConfigProvider, private val localStorage: ILocalStorage, private val ethereumKitManager: IEthereumKitManager) {

    fun adapterForCoin(coinType: CoinType, authData: AuthData): IAdapter? = when (coinType) {
        is CoinType.Bitcoin -> {
            BitcoinAdapter.createBitcoin(authData.words, appConfigProvider.testMode, localStorage.isNewWallet, authData.walletId)
        }
        is CoinType.BitcoinCash -> {
            BitcoinAdapter.createBitcoinCash(authData.words, appConfigProvider.testMode, localStorage.isNewWallet, authData.walletId)
        }
        is CoinType.Ethereum -> {
            EthereumAdapter.adapter(ethereumKitManager.ethereumKit(authData))
        }
        is CoinType.Erc20 -> {
            Erc20Adapter.adapter(ethereumKitManager.ethereumKit(authData), coinType.address, coinType.decimal)
        }
    }

    fun unlinkAdapter(adapter: IAdapter) {
        adapter.stop()

        when (adapter) {
            is EthereumBaseAdapter -> {
                ethereumKitManager.unlink()
            }
        }
    }
}
