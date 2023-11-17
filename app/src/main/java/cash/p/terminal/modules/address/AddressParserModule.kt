package cash.p.terminal.modules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.factories.AddressParserFactory
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

object AddressParserModule {
    class Factory(private val blockchainType: BlockchainType, private val prefilledAmount: BigDecimal?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressParserViewModel(AddressParserFactory.parser(blockchainType), prefilledAmount) as T
        }
    }
}
