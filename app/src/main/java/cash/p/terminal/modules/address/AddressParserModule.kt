package cash.p.terminal.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

object AddressParserModule {
    class Factory(private val token: Token, private val prefilledAmount: BigDecimal?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressParserViewModel(AddressUriParser(token.blockchainType, token.type), prefilledAmount) as T
        }
    }
}
