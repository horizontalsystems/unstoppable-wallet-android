package io.horizontalsystems.walletkit.modules.multiswap

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.isEvm
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.math.BigDecimal

/**
 * A split swap divides one deposit between several outputs of the same buy token, each
 * delivered to its own address. The shares are percents of the deposit; what each output
 * receives is the provider's number, never computed here.
 */
object SplitSwapRules {
    /** Outputs a split has. The records and the provider layer take any count the server allows. */
    const val OUTPUT_COUNT = 2
    const val PERCENT_MIN = 10
    const val PERCENT_MAX = 90
    const val PERCENT_STEP = 10
    const val PERCENT_DEFAULT = 50

    /** Shares of a two-output split from the first output's share. */
    fun percents(first: Int): List<Int> = listOf(first, 100 - first)

    /** Whether [percents] can be sent: at least two shares, each positive, summing to exactly 100. */
    fun isValid(percents: List<Int>): Boolean =
        percents.size >= 2 && percents.all { it > 0 } && percents.sum() == 100

    fun requireValid(percents: List<Int>) {
        if (!isValid(percents)) throw IllegalArgumentException("Split shares must be positive and sum to 100: $percents")
    }

    /**
     * Positions of recipients that repeat an EARLIER recipient's address, so the first of a
     * pair is never flagged. EVM addresses compare case-insensitively; every other chain's
     * encoding is case-sensitive.
     */
    fun duplicateIndexes(addresses: List<String?>, blockchainType: BlockchainType?): Set<Int> {
        val seen = mutableSetOf<String>()
        val duplicates = mutableSetOf<Int>()
        addresses.forEachIndexed { index, address ->
            if (address.isNullOrBlank()) return@forEachIndexed
            val key = if (blockchainType?.isEvm == true) address.lowercase() else address
            if (!seen.add(key)) duplicates.add(index)
        }
        return duplicates
    }
}

/** One recipient of a split as entered before confirmation: its share and where it is paid. */
@Serializable
data class SplitSwapRecipient(
    val percent: Int,
    val address: Address,
)

/**
 * One output of a previewed or committed split: its position in the request, its share, its
 * destination and what it is expected to receive. [uuid] is the output's own swap record once
 * the order exists.
 */
data class SplitSwapOutput(
    val index: Int,
    val percent: Int,
    val address: String,
    val amountOut: BigDecimal,
    val uuid: String? = null,
)

/** The route a split quote selected, carried to the confirmation with the shares it was priced for. */
data class SplitSwapQuoteExtraData(
    val route: UnstoppableAPI.Response.SplitRoute,
    val percents: List<Int>,
) : SwapQuote.ExtraData {
    /** Expected amount per output, in request order. */
    val outputAmounts: List<BigDecimal>
        get() = route.outputsByIndex.map { it.expectedBuyAmount ?: BigDecimal.ZERO }
}

/**
 * A refused split rate or preview. The server answers a non-2xx whose body is either the
 * { providerErrors } list a /v3/rate carries, or a validation { message }. [errorCode] is the
 * first provider error's code (`amountTooSmall`, `routeNotFound`, `invalidParams`) when there is one.
 */
class SplitRateException(
    val providerErrors: List<UnstoppableAPI.Response.ProviderError>,
    message: String?,
    cause: Throwable? = null,
) : Exception(message ?: providerErrors.firstOrNull()?.resolvedMessage ?: cause?.message, cause) {
    val errorCode: String?
        get() = providerErrors.firstOrNull()?.errorCode

    companion object {
        private val gson = Gson()

        fun from(error: Throwable): SplitRateException {
            if (error is SplitRateException) return error
            if (error !is HttpException) return SplitRateException(emptyList(), null, error)

            val body = try {
                error.response()?.errorBody()?.string()
            } catch (e: Throwable) {
                null
            }
            val json = try {
                body?.let { JsonParser.parseString(it) }?.takeIf { it.isJsonObject }?.asJsonObject
            } catch (e: Throwable) {
                null
            }

            val providerErrors = try {
                json?.getAsJsonArray("providerErrors")?.map { gson.fromJson(it, UnstoppableAPI.Response.ProviderError::class.java) }
            } catch (e: Throwable) {
                null
            }.orEmpty()
            val message = listOf("message", "error", "reason")
                .firstNotNullOfOrNull { key -> json?.get(key)?.takeIf { it.isJsonPrimitive }?.asString }

            return SplitRateException(providerErrors, message, error)
        }
    }
}
