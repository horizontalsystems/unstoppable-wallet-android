package io.horizontalsystems.walletkit.modules.multiswap.history

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * One output of a split swap as the record keeps it: the share and destination the user chose,
 * the output's own swap id, and what tracking has reported for it so far. Stored as JSON in
 * `SwapRecord.splitOutputs`.
 */
data class SwapRecordOutput(
    val index: Int,
    val uuid: String?,
    val percent: Int,
    val address: String,
    // Expected at commit, replaced by what the payout delivered once it is observed.
    val amountOut: String?,
    // SwapStatus.name of this output alone.
    val status: String,
    // The payout transaction, once the provider reports it.
    val txHash: String? = null,
) {
    val swapStatus: SwapStatus
        get() = runCatching { SwapStatus.valueOf(status) }.getOrDefault(SwapStatus.Depositing)

    companion object {
        private val gson = Gson()
        private val listType = object : TypeToken<List<SwapRecordOutput>>() {}.type

        fun toJson(outputs: List<SwapRecordOutput>): String = gson.toJson(outputs, listType)

        // Unreadable JSON (a record written by a newer app version) reads as no outputs rather
        // than crashing history.
        fun fromJson(json: String?): List<SwapRecordOutput> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                gson.fromJson<List<SwapRecordOutput>>(json, listType).orEmpty().sortedBy { it.index }
            } catch (e: Throwable) {
                emptyList()
            }
        }
    }
}
