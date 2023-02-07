package cash.p.terminal.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.factories.AddressParserFactory
import io.horizontalsystems.marketkit.models.BlockchainType

object AddressParserModule {
    class Factory(private val blockchainType: BlockchainType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val factory = AddressParserFactory()
            return AddressParserViewModel(factory.parser(blockchainType)) as T
        }
    }
}
