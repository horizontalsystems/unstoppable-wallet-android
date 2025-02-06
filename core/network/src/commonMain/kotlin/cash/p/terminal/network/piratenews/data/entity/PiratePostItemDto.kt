package cash.p.terminal.network.piratenews.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal class PiratePostItemDto(
    val id: Int,
    val date: String,
    val title: StringRendered,
    val link: String,
    val excerpt: StringRendered
)

@Serializable
internal class StringRendered(
    val rendered: String
)