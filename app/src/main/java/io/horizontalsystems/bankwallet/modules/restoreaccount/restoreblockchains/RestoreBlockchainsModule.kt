package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import io.horizontalsystems.bankwallet.modules.market.ImageSource

object RestoreBlockchainsModule

data class CoinViewItem<T>(
    val item: T,
    val imageSource: ImageSource,
    val title: String,
    val subtitle: String,
    val enabled: Boolean,
    val hasSettings: Boolean = false,
    val hasInfo: Boolean = false,
    val label: String? = null,
)
