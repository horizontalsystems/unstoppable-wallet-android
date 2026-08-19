package io.horizontalsystems.walletkit.modules.sendevmtransaction

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.modules.send.SendModule

data class SectionViewItem(
    val viewItems: List<ViewItem>
)

sealed class ViewItem {
    class Subhead(val title: String, val value: String, val iconRes: Int? = null) : ViewItem()
    class Value(
        val title: String,
        val value: String,
        val type: ValueType,
    ) : ViewItem()

    class Amount(
        val fiatAmount: String?,
        val coinAmount: String,
        val type: ValueType,
        val token: Token
    ) : ViewItem()

    class AmountWithTitle(
        val fiatAmount: String?,
        val coinAmount: String,
        val type: ValueType,
        val token: Token,
        val title: String,
        val badge: String?
    ) : ViewItem()

    class NftAmount(
        val iconUrl: String?,
        val amount: String,
        val type: ValueType,
    ) : ViewItem()

    class Address(val title: String, val address: String, val contact: String? = null) : ViewItem()
    class Input(val title: String, val value: String) : ViewItem()
    class TokenItem(val token: Token) : ViewItem()
    class Fee(val networkFee: SendModule.AmountData) : ViewItem()

    // A prominent alert banner (as opposed to a title/value row). `critical` picks red vs amber.
    // Used e.g. to warn that a Solana transaction couldn't be decoded (blind signing).
    class Alert(val title: String, val text: String, val critical: Boolean) : ViewItem()
}

enum class ValueType {
    Regular, Disabled, Outgoing, Incoming, Warning, Forbidden
}