package cash.p.terminal.modules.nft.collection.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.nft.NftEventsProvider
import cash.p.terminal.entities.nft.NftEventMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.modules.balance.BalanceXRateRepository
import cash.p.terminal.modules.coin.ContractInfo
import io.horizontalsystems.marketkit.models.BlockchainType

class NftCollectionEventsModule {

    class Factory(
        private val eventListType: NftEventListType,
        private val defaultEventType: NftEventMetadata.EventType = NftEventMetadata.EventType.Sale
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = NftCollectionEventsService(
                eventListType,
                defaultEventType,
                NftEventsProvider(App.marketKit),
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )
            return NftCollectionEventsViewModel(service) as T
        }
    }
}

enum class SelectorDialogState {
     Closed, Opened
}

sealed class NftEventListType {
    data class Collection(
        val blockchainType: BlockchainType,
        val providerUid: String,
        val contracts: List<ContractInfo>
    ) : NftEventListType()

    data class Asset(val nftUid: NftUid) : NftEventListType()
}
