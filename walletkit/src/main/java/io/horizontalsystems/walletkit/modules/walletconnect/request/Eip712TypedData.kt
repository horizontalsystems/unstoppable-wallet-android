package io.horizontalsystems.walletkit.modules.walletconnect.request

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigInteger

/**
 * The parts of an EIP-712 payload a user needs in order to judge what they are signing.
 *
 * A typed-data request is signed against a domain, and the domain is what binds the signature to a
 * particular contract and chain. Shown only the message body, a user cannot tell a request bound to
 * the dApp they are using from one bound to an attacker's contract, or to a different chain where
 * the same signature means something else.
 */
data class Eip712TypedData(
    val domainName: String?,
    val chainId: Long?,
    val verifyingContract: String?,
    val primaryType: String?,
    val permit: Eip712Permit? = null,
)

/**
 * A token approval granted by signature rather than by a transaction.
 *
 * Nothing appears on chain when this is signed, so the usual approval confirmation never runs and
 * the allowance is invisible afterwards until it is spent. That makes the amount and the spender
 * the two things worth reading, and an unlimited amount the thing worth refusing.
 */
data class Eip712Permit(
    val token: String?,
    val spender: String?,
    val amount: BigInteger?,
    val deadlineSeconds: Long?,
) {
    /**
     * EIP-2612 amounts are uint256 and Permit2 amounts are uint160; both use the type's maximum to
     * mean "no limit", so each is checked against its own ceiling.
     */
    val unlimited: Boolean
        get() = amount != null && (amount == MAX_UINT256 || amount == MAX_UINT160)

    companion object {
        private val MAX_UINT256 = BigInteger.TWO.pow(256) - BigInteger.ONE
        private val MAX_UINT160 = BigInteger.TWO.pow(160) - BigInteger.ONE
    }
}

object Eip712Parser {

    /**
     * Reads the domain and primary type out of a typed-data payload for display. Returns null when
     * the payload cannot be read; this never affects what is signed, which stays the raw request.
     */
    fun parse(rawJson: String): Eip712TypedData? = try {
        val root = JsonParser.parseString(rawJson).asJsonObject
        val domain = root.get("domain")?.takeIf { it.isJsonObject }?.asJsonObject

        val verifyingContract = domain?.get("verifyingContract").asStringOrNull()
        val primaryType = root.get("primaryType").asStringOrNull()

        val parsed = Eip712TypedData(
            domainName = domain?.get("name").asStringOrNull(),
            chainId = domain?.get("chainId").asChainIdOrNull(),
            verifyingContract = verifyingContract,
            primaryType = primaryType,
            permit = parsePermit(root, primaryType, verifyingContract),
        )

        // Nothing recognisable was found, so there is nothing worth showing.
        parsed.takeIf {
            it.domainName != null || it.chainId != null ||
                    it.verifyingContract != null || it.primaryType != null
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Reads the approval out of the two permit shapes in common use: EIP-2612 `Permit`, where the
     * token is the contract being signed against, and Permit2 `PermitSingle`, where it sits inside
     * `details`. Anything else returns null and is shown as ordinary typed data.
     *
     * `PermitBatch` is not handled — it grants several allowances at once and cannot be summarised
     * in one row, so it is better shown as raw data than as a summary that omits entries.
     */
    private fun parsePermit(
        root: JsonObject,
        primaryType: String?,
        verifyingContract: String?
    ): Eip712Permit? {
        val message = root.get("message")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null

        return when (primaryType) {
            "Permit" -> Eip712Permit(
                token = verifyingContract,
                spender = message.get("spender").asStringOrNull(),
                amount = message.get("value").asBigIntegerOrNull(),
                deadlineSeconds = message.get("deadline").asLongOrNull(),
            )

            "PermitSingle" -> {
                val details = message.get("details")?.takeIf { it.isJsonObject }?.asJsonObject
                Eip712Permit(
                    token = details?.get("token").asStringOrNull(),
                    spender = message.get("spender").asStringOrNull(),
                    amount = details?.get("amount").asBigIntegerOrNull(),
                    // The allowance's own lifetime, not sigDeadline, which only bounds how long the
                    // signature may be submitted.
                    deadlineSeconds = details?.get("expiration").asLongOrNull(),
                )
            }

            else -> null
        }
    }

    /** Permit amounts exceed what JSON numbers hold, so they arrive as decimal strings. */
    private fun JsonElement?.asBigIntegerOrNull(): BigInteger? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null

        return try {
            BigInteger(primitive.asString.trim())
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun JsonElement?.asLongOrNull(): Long? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null

        return try {
            primitive.asString.trim().toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    // Only genuine strings: Gson stringifies booleans and numbers too, so a crafted
    // "primaryType": true would otherwise be displayed as "true" as if the dApp had sent it.
    private fun JsonElement?.asStringOrNull(): String? = this
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    /** chainId is a number in the spec, but dApps also send it as a decimal or hex string. */
    private fun JsonElement?.asChainIdOrNull(): Long? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null

        return try {
            when {
                primitive.isNumber -> primitive.asLong
                primitive.isString -> primitive.asString.trim().let { raw ->
                    if (raw.startsWith("0x", ignoreCase = true)) {
                        raw.substring(2).toLong(16)
                    } else {
                        raw.toLong()
                    }
                }

                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
