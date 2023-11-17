package cash.p.terminal.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.IAddressParser
import cash.p.terminal.entities.AddressUriParserResult
import cash.p.terminal.ui.compose.components.TextPreprocessor
import java.math.BigDecimal
import java.util.UUID

class AddressParserViewModel(private val parser: IAddressParser, prefilledAmount: BigDecimal?) : ViewModel(), TextPreprocessor {
    private var lastEnteredText: String? = null

    var amountUnique by mutableStateOf<AmountUnique?>(prefilledAmount?.let { AmountUnique(it) })
        private set

    override fun process(text: String): String {
        var processed = text
        if (lastEnteredText.isNullOrBlank()) {

            when (val addressData = parser.parse(text)) {
                is AddressUriParserResult.Data -> {
                    amountUnique = addressData.addressData.amount?.let { AmountUnique(it) }
                    processed = addressData.addressData.address
                }

                else -> {

                }
            }
        }

        lastEnteredText = text

        return processed
    }

}

data class AmountUnique(val amount: BigDecimal, val id: Long = UUID.randomUUID().leastSignificantBits)
