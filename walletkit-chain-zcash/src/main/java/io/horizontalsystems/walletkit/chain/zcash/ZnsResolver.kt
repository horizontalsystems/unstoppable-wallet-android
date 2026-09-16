package io.horizontalsystems.walletkit.chain.zcash

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Resolves Zcash Names (ZNS) to unified addresses through the hosted indexer.
 *
 * The indexer speaks JSON-RPC 2.0. A `resolve` query for a name returns a single
 * registration or `null`; every registration carries an Ed25519 signature that is
 * verified here before the address is handed out, so a compromised indexer cannot
 * redirect an admin-signed name without the admin's private key.
 *
 * See https://www.zcashnames.com/docs/protocol/signatures
 */
class ZnsResolver(
    private val url: String = MAINNET_URL,
    private val adminPubkey: String = ADMIN_PUBKEY,
) {

    class RpcError(message: String) : Exception(message)
    class InvalidSignature(name: String) : Exception("ZNS registration for \"$name\" has an invalid signature")

    data class Registration(
        val name: String,
        val address: String,
        val nonce: Long,
        val lastAction: String,
        val signature: String?,
        val pubkey: String?,
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns the unified address bound to [name], or null when the name is not registered.
     *
     * @param name a normalized name (see [ZnsName.normalize]); the indexer does not normalize
     * @throws IOException when the indexer is unreachable or answers with a non-2xx status
     * @throws RpcError when the indexer returns a JSON-RPC error or a malformed body
     * @throws InvalidSignature when the registration's signature does not verify
     */
    fun resolve(name: String): String? {
        val registration = fetch(name) ?: return null
        return validate(name, registration, adminPubkey)
    }

    private fun fetch(name: String): Registration? {
        val requestBody = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", 1)
            addProperty("method", "resolve")
            add("params", JsonObject().apply {
                addProperty("query", name)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                throw IOException("Unexpected response: ${response.code}")
            }

            return parseResolveResponse(responseBody)
        }
    }

    companion object {
        const val MAINNET_URL = "https://main.zcashnames.com"

        private const val PUBKEY_SIZE = 32
        private const val SIGNATURE_SIZE = 64

        /** Ed25519 public key of the ZNS registrar, base64. Reported by the indexer's `status` method. */
        const val ADMIN_PUBKEY = "zobrGyAwpM3mtC0Vo4UOk0bc9Ygg0gdDeD8dCQAOXI4="

        /** Unified incoming viewing key of the mainnet registry, for reference and self-hosted indexer checks. */
        const val MAINNET_UIVK =
            "uivk1gl26qy0xjja7lqhyg3pf0x4j4j66kqwewrjkdcg28eqq4wgtzjmujpee7x9cs2ec9xhnlgrm8ptlw8z80j2aryw8nqtssser2ys778a0s00uvgkdjnfr58sndhfvc3f4zqjs6ywva6"

        /**
         * Parses the body of a `resolve` call made with a name query.
         *
         * @return the registration, or null when `result` is null (name not registered)
         * @throws RpcError on a JSON-RPC error object, a malformed body, or a result that is not
         * a registration object (an address query returns an array; we never send one)
         */
        fun parseResolveResponse(body: String): Registration? {
            val json = try {
                JsonParser.parseString(body).asJsonObject
            } catch (e: Exception) {
                throw RpcError("Malformed response")
            }

            json.get("error")?.takeIf { it.isJsonObject }?.asJsonObject?.let { error ->
                val code = error.get("code")?.asString
                val message = error.get("message")?.asString
                throw RpcError("JSON-RPC error $code: $message")
            }

            val result = json.get("result")
            if (result == null || result.isJsonNull) return null
            if (!result.isJsonObject) throw RpcError("Unexpected result shape")

            val obj = result.asJsonObject
            return try {
                Registration(
                    name = obj.get("name").asString,
                    address = obj.get("address").asString,
                    nonce = obj.get("nonce").asLong,
                    lastAction = obj.get("last_action").asString,
                    signature = obj.get("signature")?.takeIf { !it.isJsonNull }?.asString,
                    pubkey = obj.get("pubkey")?.takeIf { !it.isJsonNull }?.asString,
                )
            } catch (e: Exception) {
                throw RpcError("Registration is missing required fields")
            }
        }

        /**
         * How far below the reported nonce [verify] searches for the nonce an UPDATE was
         * actually signed with (see the note on [verify]).
         */
        private const val NONCE_SEARCH_DEPTH = 64L

        /**
         * Checks that [registration] answers the query for [name] and carries a valid
         * registrar signature over its address, and returns that address.
         *
         * @throws RpcError when the registration is for a different name
         * @throws InvalidSignature when the signature does not authenticate the address
         */
        fun validate(name: String, registration: Registration, adminPubkey: String): String {
            if (registration.name != name) {
                throw RpcError("Registration name \"${registration.name}\" does not match query \"$name\"")
            }
            if (!verify(registration, adminPubkey)) {
                throw InvalidSignature(name)
            }
            return registration.address
        }

        /**
         * The ASCII pre-image the registrar signed, selected by `last_action`.
         *
         * Only actions whose pre-image covers the address are accepted. A DELIST pre-image
         * is `DELIST:{name}:{nonce}`, so a DELIST signature says nothing about the address the
         * indexer attaches to it; LIST and RELEASE never appear on a registration.
         */
        fun preImage(registration: Registration, nonce: Long = registration.nonce): String? =
            with(registration) {
                when (lastAction) {
                    "CLAIM" -> "CLAIM:$name:$address"
                    "BUY" -> "BUY:$name:$address"
                    "UPDATE" -> "UPDATE:$name:$address:$nonce"
                    else -> null
                }
            }

        /**
         * Verifies that the registrar's Ed25519 signature binds the registration's name to
         * its address.
         *
         * Only admin-signed registrations are accepted. A sovereign registration carries the
         * owner's key in `pubkey`, but nothing signed by the registrar vouches for that key,
         * so verifying against it would only prove the response is self-consistent.
         *
         * Nonce caveat: the `nonce` a registration reports is the registry's current value,
         * and a later LIST bumps it without changing `last_action` or `signature`. So for
         * UPDATE the signed nonce can be lower than the reported one, and the indexer exposes
         * no way to recover it. The pre-image is therefore tried with the reported nonce first
         * and then with each lower value down to zero, bounded by [NONCE_SEARCH_DEPTH]. Any
         * match still proves the registrar signed this exact name-to-address binding.
         */
        fun verify(registration: Registration, adminPubkey: String): Boolean {
            val signature = registration.signature ?: return false
            if (registration.pubkey != null) return false

            val signatureBytes: ByteArray
            val pubkeyBytes: ByteArray
            try {
                signatureBytes = Base64.getDecoder().decode(signature)
                pubkeyBytes = Base64.getDecoder().decode(adminPubkey)
            } catch (e: IllegalArgumentException) {
                return false
            }
            if (signatureBytes.size != SIGNATURE_SIZE || pubkeyBytes.size != PUBKEY_SIZE) {
                return false
            }

            val usesNonce = registration.lastAction == "UPDATE"
            val lowestNonce = if (usesNonce) maxOf(0L, registration.nonce - NONCE_SEARCH_DEPTH) else registration.nonce

            for (nonce in registration.nonce downTo lowestNonce) {
                val preImage = preImage(registration, nonce) ?: return false
                if (verifySignature(preImage, signatureBytes, pubkeyBytes)) return true
            }
            return false
        }

        private fun verifySignature(preImage: String, signature: ByteArray, pubkey: ByteArray): Boolean {
            return try {
                val message = preImage.toByteArray(Charsets.US_ASCII)
                val signer = Ed25519Signer()
                signer.init(false, Ed25519PublicKeyParameters(pubkey, 0))
                signer.update(message, 0, message.size)
                signer.verifySignature(signature)
            } catch (e: Exception) {
                false
            }
        }
    }
}
