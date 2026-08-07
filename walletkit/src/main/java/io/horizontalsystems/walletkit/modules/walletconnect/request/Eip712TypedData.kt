package io.horizontalsystems.walletkit.modules.walletconnect.request

import com.google.gson.JsonElement
import com.google.gson.JsonParser

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
)

object Eip712Parser {

    /**
     * Reads the domain and primary type out of a typed-data payload for display. Returns null when
     * the payload cannot be read; this never affects what is signed, which stays the raw request.
     */
    fun parse(rawJson: String): Eip712TypedData? = try {
        val root = JsonParser.parseString(rawJson).asJsonObject
        val domain = root.get("domain")?.takeIf { it.isJsonObject }?.asJsonObject

        val parsed = Eip712TypedData(
            domainName = domain?.get("name").asStringOrNull(),
            chainId = domain?.get("chainId").asChainIdOrNull(),
            verifyingContract = domain?.get("verifyingContract").asStringOrNull(),
            primaryType = root.get("primaryType").asStringOrNull(),
        )

        // Nothing recognisable was found, so there is nothing worth showing.
        parsed.takeIf {
            it.domainName != null || it.chainId != null ||
                    it.verifyingContract != null || it.primaryType != null
        }
    } catch (e: Exception) {
        null
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
