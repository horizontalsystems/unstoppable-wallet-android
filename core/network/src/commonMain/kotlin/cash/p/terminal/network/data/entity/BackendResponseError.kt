package cash.p.terminal.network.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendResponseError(
    @SerialName("message") val message: String?
)