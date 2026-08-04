package io.horizontalsystems.walletkit.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.utils.AddressUriParser
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.TokenQuery

object AddressInputModule {

    class FactoryToken(private val tokenQuery: TokenQuery, private val coinCode: String, private val initial: Address?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val blockchainType = tokenQuery.blockchainType
            val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
            val udnHandler = AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))

            plainAddressHandlers(blockchainType).forEach {
                addressParserChain.addHandler(it)
            }

            val addressUriParser = AddressUriParser(blockchainType, tokenQuery.tokenType)
            val addressViewModel = AddressViewModel(
                blockchainType,
                addressUriParser,
                addressParserChain,
                initial
            )

            return addressViewModel as T
        }
    }

}
