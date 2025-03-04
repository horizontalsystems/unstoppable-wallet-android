package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class GraphUrlsDto(
    val day: String,
    val hour: String,
    val max: String,
    val month: String,
    val week: String,
    val year: String
)
