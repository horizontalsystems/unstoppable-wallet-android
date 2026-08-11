package io.horizontalsystems.walletkit.modules.walletconnect.request

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigInteger
import java.util.Date

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
    /**
     * How long the signature may be submitted for: EIP-2612 `deadline`, Permit2 `sigDeadline`. It
     * bounds the window for using the signature, not the life of the allowance it grants.
     */
    val signatureDeadline: BigInteger?,
    /**
     * When the allowance itself lapses. Permit2 only — an EIP-2612 allowance is written by
     * `permit()` and then persists until it is changed or revoked, so there is nothing to show.
     */
    val allowanceExpires: BigInteger?,
    /**
     * Whether [amount] is the maximum of its own type. EIP-2612 amounts are uint256 and Permit2
     * amounts are uint160, and each uses its own ceiling to mean "no limit" — so this is decided
     * while the shape is still known rather than by testing against both.
     */
    val unlimited: Boolean,
)

// Permit2 is deployed at one address across chains, so a PermitSingle addressed anywhere else is
// not Permit2 regardless of the structs it declares.
private const val PERMIT2_CONTRACT = "0x000000000022D473030F116dDEE9F6B43aC78BA3"

// EIP-2612: Permit(address owner,address spender,uint256 value,uint256 nonce,uint256 deadline)
private val EIP2612_PERMIT_FIELDS = listOf(
    "owner" to "address",
    "spender" to "address",
    "value" to "uint256",
    "nonce" to "uint256",
    "deadline" to "uint256",
)

// Permit2: PermitSingle(PermitDetails details,address spender,uint256 sigDeadline)
private val PERMIT2_SINGLE_FIELDS = listOf(
    "details" to "PermitDetails",
    "spender" to "address",
    "sigDeadline" to "uint256",
)

// Permit2: PermitDetails(address token,uint160 amount,uint48 expiration,uint48 nonce)
private val PERMIT2_DETAILS_FIELDS = listOf(
    "token" to "address",
    "amount" to "uint160",
    "expiration" to "uint48",
    "nonce" to "uint48",
)

private val MAX_UINT256 = BigInteger.TWO.pow(256) - BigInteger.ONE
private val MAX_UINT160 = BigInteger.TWO.pow(160) - BigInteger.ONE

/** Beyond this a value times 1000 no longer fits in the Long that Date is built from. */
private val MAX_DATE_SECONDS = BigInteger.valueOf(Long.MAX_VALUE / 1000)

/**
 * Reads a uint256 timestamp as a date, or null when it does not denote one.
 *
 * These fields are uint256, and dApps use a huge value to mean "no deadline". Anything outside the
 * range a Date can hold is such a sentinel rather than a real time, so it is reported as absent
 * instead of being wrapped into a nonsensical date.
 */
fun BigInteger.asTimestampOrNull(): Date? =
    if (signum() > 0 && this <= MAX_DATE_SECONDS) Date(toLong() * 1000) else null

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
        val types = root.get("types")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null

        return when (primaryType) {
            "Permit" -> {
                // The name alone proves nothing: a dApp may declare any struct it likes and call it
                // Permit. Only the canonical field list makes this an EIP-2612 approval, and a
                // friendly summary over something else would lend it credibility it has not earned.
                if (!types.definesType("Permit", EIP2612_PERMIT_FIELDS)) return null

                val amount = message.get("value").asBigIntegerOrNull()
                Eip712Permit(
                    token = verifyingContract,
                    spender = message.get("spender").asStringOrNull(),
                    amount = amount,
                    signatureDeadline = message.get("deadline").asBigIntegerOrNull(),
                    // Nothing to show: the allowance outlives the deadline that gated the signature.
                    allowanceExpires = null,
                    unlimited = amount != null && amount == MAX_UINT256,
                )
            }

            "PermitSingle" -> {
                // Both structs must match, and the request must be addressed to Permit2 itself.
                // Anything else declaring these shapes is a different contract wearing the name.
                if (!types.definesType("PermitSingle", PERMIT2_SINGLE_FIELDS)) return null
                if (!types.definesType("PermitDetails", PERMIT2_DETAILS_FIELDS)) return null
                if (!verifyingContract.equals(PERMIT2_CONTRACT, ignoreCase = true)) return null

                val details = message.get("details")?.takeIf { it.isJsonObject }?.asJsonObject
                val amount = details?.get("amount").asBigIntegerOrNull()
                Eip712Permit(
                    token = details?.get("token").asStringOrNull(),
                    spender = message.get("spender").asStringOrNull(),
                    amount = amount,
                    signatureDeadline = message.get("sigDeadline").asBigIntegerOrNull(),
                    allowanceExpires = details?.get("expiration").asBigIntegerOrNull(),
                    unlimited = amount != null && amount == MAX_UINT160,
                )
            }

            else -> null
        }
    }

    /**
     * True when `types` declares [name] with exactly [fields], in order. EIP-712 hashes the type
     * definition along with the message, so the declaration is what the signature actually commits
     * to — matching it is what distinguishes a real approval from a struct that merely borrows the
     * name.
     */
    private fun JsonObject.definesType(name: String, fields: List<Pair<String, String>>): Boolean {
        val declared = get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: return false
        if (declared.size() != fields.size) return false

        return fields.withIndex().all { (index, expected) ->
            val field = declared[index]?.takeIf { it.isJsonObject }?.asJsonObject
            field?.get("name").asStringOrNull() == expected.first &&
                    field?.get("type").asStringOrNull() == expected.second
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
