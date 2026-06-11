package cash.p.terminal.network.exolix.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class ExolixNetworkDto(
    val network: String,
    val name: String,
    val shortName: String? = null,
    val memoNeeded: Boolean = false,
    val memoName: String? = null,
    val contract: String? = null,
)
