package cash.p.terminal.modules.send.address

import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.core.utils.ToncoinUriParser
import cash.p.terminal.modules.address.AddressValidationException
import cash.p.terminal.wallet.title
import io.horizontalsystems.core.entities.BlockchainType

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
                throw AddressValidationException.Invalid(
                    Throwable("Invalid Blockchain Type"),
                    blockchainType.title
                )
            }

            AddressUriResult.InvalidTokenType -> {
                throw AddressValidationException.Invalid(
                    Throwable("Invalid Token Type"),
                    blockchainType.title
                )
            }

            AddressUriResult.NoUri, AddressUriResult.WrongUri -> {
                return text
            }
        }
    }
}
