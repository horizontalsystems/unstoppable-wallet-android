package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.xxxkit.models.BlockchainType
import io.horizontalsystems.xxxkit.models.TokenQuery

object AddressInputModule {

    class Factory(private val tokenQuery: TokenQuery, private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressViewModel = AddressViewModel()

            addressViewModel.addAddressHandler(AddressHandlerEns())
            addressViewModel.addAddressHandler(AddressHandlerUdn(tokenQuery, coinCode))

            when (tokenQuery.blockchainType) {
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Litecoin,
                BlockchainType.Dash,
                BlockchainType.Zcash,
                BlockchainType.BinanceChain -> {
                    addressViewModel.addAddressHandler(AddressHandlerPure())
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Optimism,
                BlockchainType.ArbitrumOne -> {
                    addressViewModel.addAddressHandler(AddressHandlerEvm())
                }
                is BlockchainType.Unsupported -> Unit
            }


            return addressViewModel as T
        }
    }

}
