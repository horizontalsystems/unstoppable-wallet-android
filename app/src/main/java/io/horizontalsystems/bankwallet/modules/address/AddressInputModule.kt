package io.horizontalsystems.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.marketkit.models.CoinType

object AddressInputModule {

    class Factory(private val coinType: CoinType, private val coinCode: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val addressViewModel = AddressViewModel()

            addressViewModel.addAddressHandler(AddressHandlerUdn(coinType, coinCode))

            when (coinType) {
                CoinType.Bitcoin,
                CoinType.BitcoinCash,
                CoinType.Litecoin,
                CoinType.Dash,
                CoinType.Zcash,
                is CoinType.Bep2 -> {
                    addressViewModel.addAddressHandler(AddressHandlerPure())
                }
                CoinType.Ethereum,
                CoinType.BinanceSmartChain,
                is CoinType.Erc20,
                is CoinType.Bep20 -> {
                    addressViewModel.addAddressHandler(AddressHandlerEvm())
                }
                else -> Unit
            }


            return addressViewModel as T
        }
    }

}
