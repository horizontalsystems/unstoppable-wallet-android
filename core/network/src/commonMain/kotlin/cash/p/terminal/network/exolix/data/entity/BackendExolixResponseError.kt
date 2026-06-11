package cash.p.terminal.network.exolix.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BackendExolixResponseError(
    val error: String? = null,
    override val message: String? = null,
    val statusCode: Int? = null,
    val errors: JsonElement? = null,
) : Throwable() {
    override fun toString(): String {
        return "BackendExolixResponseError(error=$error, message=$message, statusCode=$statusCode, errors=$errors)"
    }
}
