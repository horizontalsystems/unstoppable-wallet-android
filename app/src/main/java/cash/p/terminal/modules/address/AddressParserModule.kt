package cash.p.terminal.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.modules.sendtokenselect.PrefilledData
import cash.p.terminal.wallet.Token

object AddressParserModule {
    class Factory(private val token: Token, private val prefilledData: PrefilledData?) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressParserViewModel(
                AddressUriParser(token.blockchainType, token.type),
                prefilledData
            ) as T
        }
    }
}
