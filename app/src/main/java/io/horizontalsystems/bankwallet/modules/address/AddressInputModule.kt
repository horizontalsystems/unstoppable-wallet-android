package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.swap.settings.AddressResolutionService
import io.horizontalsystems.marketkit.models.CoinType

object AddressInputModule {

    class Factory(private val coinType: CoinType, private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressViewModel = AddressViewModel()

            val coinCode = AddressResolutionService.getChainCoinCode(coinType) ?: coinCode
            addressViewModel.addAddressHandler(AddressHandlerUdn(coinCode))

            when (coinType) {
                CoinType.Bitcoin -> {
                    addressViewModel.addAddressHandler(AddressHandlerPure())
                }
                CoinType.BitcoinCash -> TODO()
                CoinType.Litecoin -> TODO()
                CoinType.Dash -> TODO()
                CoinType.Zcash -> TODO()
                CoinType.Ethereum -> {
                    addressViewModel.addAddressHandler(AddressHandlerEvm())
                }
                CoinType.BinanceSmartChain -> TODO()
                is CoinType.Erc20 -> TODO()
                is CoinType.Bep20 -> TODO()
                is CoinType.Bep2 -> TODO()
                is CoinType.ArbitrumOne -> TODO()
                is CoinType.Avalanche -> TODO()
                is CoinType.Fantom -> TODO()
                is CoinType.HarmonyShard0 -> TODO()
                is CoinType.HuobiToken -> TODO()
                is CoinType.Iotex -> TODO()
                is CoinType.Moonriver -> TODO()
                is CoinType.OkexChain -> TODO()
                is CoinType.PolygonPos -> TODO()
                is CoinType.Solana -> TODO()
                is CoinType.Sora -> TODO()
                is CoinType.Tomochain -> TODO()
                is CoinType.Xdai -> TODO()
                is CoinType.Unsupported -> TODO()
            }


            return addressViewModel as T
        }
    }

}
