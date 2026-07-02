package io.horizontalsystems.walletkit.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.IAddressParser
import io.horizontalsystems.walletkit.core.utils.AddressUriResult
import io.horizontalsystems.walletkit.ui.compose.components.TextPreprocessor
import java.math.BigDecimal
import java.util.UUID

class AddressParserViewModel(private val parser: IAddressParser, prefilledAmount: BigDecimal?) : ViewModel(), TextPreprocessor {
    private var lastEnteredText: String? = null

    var amountUnique by mutableStateOf<AmountUnique?>(prefilledAmount?.let { AmountUnique(it) })
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

data class AmountUnique(val amount: BigDecimal, val id: Long = UUID.randomUUID().leastSignificantBits)
