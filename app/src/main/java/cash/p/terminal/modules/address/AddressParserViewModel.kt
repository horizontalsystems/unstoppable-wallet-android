package cash.p.terminal.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.IAddressParser
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.modules.sendtokenselect.PrefilledData
import cash.p.terminal.ui.compose.components.TextPreprocessor
import java.math.BigDecimal
import java.util.UUID

class AddressParserViewModel(
    private val parser: IAddressParser,
    prefilledData: PrefilledData?
) :
    ViewModel(), TextPreprocessor {
    private var lastEnteredText: String? = null

    var amountUnique by mutableStateOf<AmountUnique?>(prefilledData?.amount?.let { AmountUnique(it) })
        private set

    override fun process(text: String): String {
        var processed = text
        if (lastEnteredText.isNullOrBlank()) {
            // parse only to get amount, full parsing done in AddressViewModel
            val addressData = parser.parse(text)
            val amount = (addressData as? AddressUriResult.Uri)?.addressUri?.amount
            if (amount != null) {
                amountUnique = AmountUnique(amount)
                processed = addressData.addressUri.address
            }
        }

        lastEnteredText = text

        return processed
    }

}

data class AmountUnique(
    val amount: BigDecimal,
    val id: Long = UUID.randomUUID().leastSignificantBits
)
