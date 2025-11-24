package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SectionViewItem

data class WCActionState(
    val runnable: Boolean,
    val items: List<SectionViewItem>
)
