package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class MasterNodesDto(
    @SerialName("ips")
    val ips: List<String>
)
