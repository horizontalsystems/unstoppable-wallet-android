package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.core.utils.AddressUriResult
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.UUID

@HiltViewModel(assistedFactory = AddressParserViewModel.Factory::class)
class AddressParserViewModel @AssistedInject constructor(
    @Assisted token: Token,
    @Assisted prefilledAmount: BigDecimal?,
) : ViewModel(), TextPreprocessor {

    @AssistedFactory
    interface Factory {
        fun create(token: Token, prefilledAmount: BigDecimal?): AddressParserViewModel
    }

    private val parser = AddressUriParser(token.blockchainType, token.type)
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
