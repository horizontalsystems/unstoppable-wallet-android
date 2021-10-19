package io.horizontalsystems.bankwallet.ui.compose

data class Select<T : WithTranslatableTitle>(
    val selected: T,
    val options: List<T>
)