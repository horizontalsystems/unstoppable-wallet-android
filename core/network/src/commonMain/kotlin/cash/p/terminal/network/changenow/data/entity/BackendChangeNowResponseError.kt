package cash.p.terminal.network.changenow.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendChangeNowResponseError(
    @SerialName("error") val error: String?,
    @SerialName("message") override val message: String?
): Throwable() {
    companion object {
        const val DEPOSIT_TOO_SMALL = "deposit_too_small"
        const val OUT_OF_RANGE = "out_of_range"
        const val NOT_VALID_REFUND_ADDRESS = "not_valid_refund_address"
        const val NOT_VALID_ADDRESS = "not_valid_address"
    }
    override fun toString(): String {
        return "BackendChangeNowResponseError(error=$error, message=$message)"
    }
}