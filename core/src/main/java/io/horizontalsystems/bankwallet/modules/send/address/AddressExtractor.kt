package io.horizontalsystems.bankwallet.modules.send.address

import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.core.utils.AddressUriResult
import io.horizontalsystems.bankwallet.core.utils.ToncoinUriParser
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
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
