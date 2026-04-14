package com.quantum.wallet.bankwallet.modules.walletconnect.request

import com.quantum.wallet.bankwallet.modules.sendevmtransaction.SectionViewItem

data class WCActionState(
    val runnable: Boolean,
    val items: List<SectionViewItem>
)
