package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.core.title
import io.horizontalsystems.walletkit.core.utils.AddressUriParser
import io.horizontalsystems.walletkit.core.utils.AddressUriResult
import io.horizontalsystems.walletkit.core.utils.ToncoinUriParser
import io.horizontalsystems.walletkit.modules.address.AddressValidationException
import io.horizontalsystems.marketkit.models.BlockchainType

class AddressExtractor(
    private val blockchainType: BlockchainType,
    private val addressUriParser: AddressUriParser,
) {
    fun extractAddressFromUri(text: String): String {
        if (blockchainType == BlockchainType.Ton && text.contains("//")) {
            ToncoinUriParser.getAddress(text)?.let { address ->
                return address
            }
        }
        when (val result = addressUriParser.parse(text)) {
            is AddressUriResult.Uri -> {
                return result.addressUri.address
            }

            AddressUriResult.InvalidBlockchainType -> {
                throw AddressValidationException.Invalid(Throwable("Invalid Blockchain Type"), blockchainType.title)
            }

            AddressUriResult.InvalidTokenType -> {
                throw AddressValidationException.Invalid(Throwable("Invalid Token Type"), blockchainType.title)
            }

            AddressUriResult.NoUri, AddressUriResult.WrongUri -> {
                return text
            }
        }
    }


}
