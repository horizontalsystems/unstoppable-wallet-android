package com.quantum.wallet.bankwallet.modules.send.address

import com.quantum.wallet.bankwallet.core.title
import com.quantum.wallet.bankwallet.core.utils.AddressUriParser
import com.quantum.wallet.bankwallet.core.utils.AddressUriResult
import com.quantum.wallet.bankwallet.core.utils.ToncoinUriParser
import com.quantum.wallet.bankwallet.modules.address.AddressValidationException
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
