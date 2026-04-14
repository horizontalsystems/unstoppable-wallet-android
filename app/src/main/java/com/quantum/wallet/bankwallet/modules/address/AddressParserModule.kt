package com.quantum.wallet.bankwallet.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

object AddressParserModule {
    class Factory(private val token: Token, private val prefilledAmount: BigDecimal?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressParserViewModel(AddressUriParser(token.blockchainType, token.type), prefilledAmount) as T
        }
    }
}
