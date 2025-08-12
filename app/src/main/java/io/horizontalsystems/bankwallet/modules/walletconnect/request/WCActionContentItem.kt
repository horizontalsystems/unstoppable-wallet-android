package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

sealed class WCActionContentItem {
    data class Fee(val networkFee: SendModule.AmountData?) : WCActionContentItem()

    data class Paragraph(
        val value: TranslatableString
    ) : WCActionContentItem()

    data class SingleLine(
        val title: TranslatableString,
        val value: TranslatableString?
    ) : WCActionContentItem()

    data class Multiline(
        val title: TranslatableString,
        val value: TranslatableString,
        val subvalue: TranslatableString,
    ) : WCActionContentItem()

    data class Section(
        val items: List<WCActionContentItem>,
        val title: TranslatableString? = null,
    ) : WCActionContentItem()
}
