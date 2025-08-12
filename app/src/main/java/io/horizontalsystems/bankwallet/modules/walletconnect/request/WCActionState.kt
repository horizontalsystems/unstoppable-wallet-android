package io.horizontalsystems.bankwallet.modules.walletconnect.request

data class WCActionState(
    val runnable: Boolean,
    val items: List<WCActionContentItem>
)
