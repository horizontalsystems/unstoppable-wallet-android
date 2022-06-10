package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAddressParser
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import java.math.BigDecimal
import java.util.*

class AddressParserViewModel(private val parser: IAddressParser) : ViewModel(), TextPreprocessor {
    private var lastEnteredText: String? = null

    var amountUnique by mutableStateOf<AmountUnique?>(null)
        private set

    override fun process(text: String): String {
        var processed = text
        if (lastEnteredText.isNullOrBlank()) {
            val addressData = parser.parse(text)

            amountUnique = addressData.amount?.let { AmountUnique(it) }
            processed = addressData.address
        }

        lastEnteredText = text

        return processed
    }

}

data class AmountUnique(val amount: BigDecimal, val id: Long = UUID.randomUUID().leastSignificantBits)
