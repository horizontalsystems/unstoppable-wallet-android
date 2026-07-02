package io.horizontalsystems.walletkit.modules.walletconnect.request

import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem

data class WCActionState(
    val runnable: Boolean,
    val items: List<SectionViewItem>
)
