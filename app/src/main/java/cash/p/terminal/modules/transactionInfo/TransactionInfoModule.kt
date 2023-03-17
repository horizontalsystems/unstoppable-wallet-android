package cash.p.terminal.modules.transactionInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType

object TransactionInfoModule {

    class Factory(private val transactionItem: TransactionItem) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val transactionSource = transactionItem.record.source
            val adapter: ITransactionsAdapter = App.transactionAdapterManager.getAdapter(transactionSource)!!
            val service = TransactionInfoService(
                transactionItem.record,
                adapter,
                App.marketKit,
                App.currencyManager,
                NftMetadataService(App.nftMetadataManager)
            )
            val factory = TransactionInfoViewItemFactory(
                App.numberFormatter,
                Translator,
                DateHelper,
                App.evmLabelManager,
                transactionSource.blockchain.type.resendable,
                App.contactsRepository,
                transactionSource.blockchain.type
            )

            return TransactionInfoViewModel(service, factory, App.contactsRepository) as T
        }

    }

    data class ExplorerData(val title: String, val url: String?)
}

sealed class TransactionStatusViewItem(val name: Int) {
    object Pending : TransactionStatusViewItem(R.string.Transactions_Pending)

    //progress in 0.0 .. 1.0
    class Processing(val progress: Float) : TransactionStatusViewItem(R.string.Transactions_Processing)
    object Completed : TransactionStatusViewItem(R.string.Transactions_Completed)
    object Failed : TransactionStatusViewItem(R.string.Transactions_Failed)
}

data class TransactionInfoItem(
    val record: TransactionRecord,
    val lastBlockInfo: LastBlockInfo?,
    val explorerData: TransactionInfoModule.ExplorerData,
    val rates: Map<String, CurrencyValue>,
    val nftMetadata: Map<NftUid, NftAssetBriefMetadata>
)

val BlockchainType.resendable: Boolean
    get() =
        when (this) {
            BlockchainType.Optimism, BlockchainType.ArbitrumOne -> false
            else -> true
        }
