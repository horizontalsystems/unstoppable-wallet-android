package cash.p.terminal.network.changenow.domain.entity

data class ChangeNowCurrency(
    val ticker: String,
    val name: String,
    val image: String,
    val hasExternalId: Boolean,
    val isExtraIdSupported: Boolean,
    val isFiat: Boolean,
    val featured: Boolean,
    val isStable: Boolean,
    val supportsFixedRate: Boolean
)