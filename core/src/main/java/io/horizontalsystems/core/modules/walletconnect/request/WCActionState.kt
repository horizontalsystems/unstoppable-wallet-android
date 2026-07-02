package io.horizontalsystems.core.modules.walletconnect.request

import io.horizontalsystems.core.modules.sendevmtransaction.SectionViewItem

data class WCActionState(
    val runnable: Boolean,
    val items: List<SectionViewItem>
)
