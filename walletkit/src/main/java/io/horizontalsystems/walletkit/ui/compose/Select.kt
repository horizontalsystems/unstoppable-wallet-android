package io.horizontalsystems.walletkit.ui.compose

data class Select<T>(
    val selected: T,
    val options: List<T>
)
