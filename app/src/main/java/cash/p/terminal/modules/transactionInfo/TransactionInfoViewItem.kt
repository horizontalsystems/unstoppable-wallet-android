package cash.p.terminal.modules.transactionInfo

import cash.p.terminal.core.stats.StatSection
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.ui_compose.ColoredValue
import io.horizontalsystems.core.entities.BlockchainType
import java.util.Date

sealed class TransactionInfoViewItem {
    class Transaction(val leftValue: String, val rightValue: String, val icon: Int?) : TransactionInfoViewItem()

    class Amount(
        val coinValue: ColoredValue,
        val fiatValue: ColoredValue,
        val coinIconUrl: String?,
        val alternativeCoinIconUrl: String?,
        val coinIconPlaceholder: Int?,
        val coinUid: String?,
        val badge: String?,
        val amountType: AmountType,
    ) : TransactionInfoViewItem()

    class NftAmount(
        val title: String,
        val nftValue: ColoredValue,
        val nftName: String?,
        val iconUrl: String?,
        val iconPlaceholder: Int?,
        val badge: String?,
    ) : TransactionInfoViewItem()

    class Value(val title: String, val value: String) : TransactionInfoViewItem()

    class PriceWithToggle(val title: String, val valueOne: String, val valueTwo: String) : TransactionInfoViewItem()

    class Address(val title: String, val value: String, val showAdd: Boolean, val blockchainType: BlockchainType, val statSection: StatSection) : TransactionInfoViewItem()

    class ContactItem(val contact: Contact) : TransactionInfoViewItem()

    class TransactionHash(val transactionHash: String) : TransactionInfoViewItem()

    class Explorer(val title: String, val url: String?) : TransactionInfoViewItem()

    class Status(val status: TransactionStatus) : TransactionInfoViewItem()

    object RawTransaction : TransactionInfoViewItem()

    class LockState(val title: String, val leftIcon: Int, val date: Date, val showLockInfo: Boolean) : TransactionInfoViewItem()

    class DoubleSpend(val transactionHash: String, val conflictingHash: String) : TransactionInfoViewItem()

    object SentToSelf : TransactionInfoViewItem()

    class SpeedUpCancel(val transactionHash: String, val blockchainType: BlockchainType) : TransactionInfoViewItem()

    class WarningMessage(val message: String) : TransactionInfoViewItem()

    class Description(val text: String) : TransactionInfoViewItem()
}

enum class AmountType {
    YouSent, YouGot, Received, Sent, Approved;
}
