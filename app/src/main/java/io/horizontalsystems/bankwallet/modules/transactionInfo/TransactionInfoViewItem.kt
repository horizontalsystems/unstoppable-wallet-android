package io.horizontalsystems.bankwallet.modules.transactionInfo

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.marketkit.models.BlockchainType
import java.util.*

sealed class TransactionInfoViewItem {
    class Transaction(val leftValue: String, val rightValue: String, val icon: Int?) : TransactionInfoViewItem()

    class Amount(val coinValue: ColoredValue, val fiatValue: ColoredValue, val coinIconUrl: String?, val coinIconPlaceholder: Int?, val coinUid: String?) : TransactionInfoViewItem()

    class NftAmount(val nftValue: ColoredValue, val iconUrl: String?, val iconPlaceholder: Int?, val nftUid: NftUid, val providerCollectionUid: String?) : TransactionInfoViewItem()

    class Value(val title: String, val value: String) : TransactionInfoViewItem()

    class Address(val title: String, val value: String, val showAdd: Boolean, val blockchainType: BlockchainType) : TransactionInfoViewItem()

    class ContactItem(val contact: Contact) : TransactionInfoViewItem()

    class TransactionHash(val transactionHash: String) : TransactionInfoViewItem()

    class Explorer(val title: String, val url: String?) : TransactionInfoViewItem()

    class Status(val status: TransactionStatus) : TransactionInfoViewItem()

    object RawTransaction : TransactionInfoViewItem()

    class LockState(val title: String, val leftIcon: Int, val date: Date, val showLockInfo: Boolean) : TransactionInfoViewItem()

    class DoubleSpend(val transactionHash: String, val conflictingHash: String) : TransactionInfoViewItem()

    object SentToSelf : TransactionInfoViewItem()

    class SpeedUpCancel(val transactionHash: String) : TransactionInfoViewItem()
}

data class ColoredValue(val value: String, val color: ColorName)

enum class ColorName {
    Remus, Lucian, Grey, Leah;

    @Composable
    fun compose() = when (this) {
        Remus -> ComposeAppTheme.colors.remus
        Lucian -> ComposeAppTheme.colors.lucian
        Leah -> ComposeAppTheme.colors.leah
        Grey -> ComposeAppTheme.colors.grey
    }
}
