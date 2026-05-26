package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.OcpPaymentRecord
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.marketkit.models.BlockchainType

object TransactionInfoModule {

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
    val nftMetadata: Map<NftUid, NftAssetBriefMetadata>,
    val hideAmount: Boolean,
    val ocpPayment: OcpPaymentRecord? = null,
)

val BlockchainType.resendable: Boolean
    get() =
        when (this) {
            BlockchainType.Optimism, BlockchainType.Base, BlockchainType.ZkSync, BlockchainType.ArbitrumOne -> false
            else -> true
        }
