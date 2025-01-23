package cash.p.terminal.network.changenow.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class ChangeNowCurrencyDto(
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