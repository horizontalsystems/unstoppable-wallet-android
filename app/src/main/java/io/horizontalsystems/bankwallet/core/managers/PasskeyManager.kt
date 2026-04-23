package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Base64
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID

class PasskeyManager {

    data class AuthResult(val entropy: ByteArray, val accountName: String?) {
        override fun equals(other: Any?) =
            other is AuthResult && entropy.contentEquals(other.entropy) && accountName == other.accountName

        override fun hashCode() = 31 * entropy.contentHashCode() + accountName.hashCode()
    }

    companion object {
        private const val RP_ID = "unstoppable.money"
        private const val RP_NAME = "Unstoppable Wallet"
        private val PRF_SALT_BYTES = "wallet".toByteArray(Charsets.UTF_8)
        private const val B64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    }

    suspend fun register(context: Context, accountName: String): ByteArray {
        val credentialManager = CredentialManager.create(context)

        val registerResponse = credentialManager.createCredential(
            context = context,
            request = CreatePublicKeyCredentialRequest(
                requestJson = buildRegisterJson(
                    challenge = randomBytes(32),
                    userId = encodeUserId(accountName),
                    accountName = accountName,
                )
            ),
        ) as CreatePublicKeyCredentialResponse

        val registrationJson = registerResponse.registrationResponseJson

        // Some authenticators (e.g. Google Password Manager) return PRF results on create.
        // Use them directly to avoid a second getCredential call, which would fail because
        // the newly created passkey may not yet be locally available for assertion.
        parsePrf(registrationJson)?.let { return it }

        // Fallback: assert immediately to get PRF entropy.
        // Used by authenticators (e.g. Samsung Pass) that only return PRF on assertion.
        val credentialId = JSONObject(registrationJson).getString("id")
        val assertResult = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest(
                listOf(GetPublicKeyCredentialOption(buildAssertJson(randomBytes(32), credentialId)))
            ),
        )
        val assertionJson = (assertResult.credential as PublicKeyCredential).authenticationResponseJson
        return parsePrf(assertionJson) ?: error("PRF output missing from assertion response")
    }

    suspend fun authenticate(context: Context): AuthResult {
        val credentialManager = CredentialManager.create(context)

        val result = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest(
                listOf(GetPublicKeyCredentialOption(buildAssertJson(randomBytes(32), credentialId = null)))
            ),
        )
        val assertionJson = (result.credential as PublicKeyCredential).authenticationResponseJson
        val entropy = parsePrf(assertionJson) ?: error("PRF output missing from assertion response")
        return AuthResult(entropy, decodeAccountName(assertionJson))
    }

    // -------------------------------------------------------------------------
    // JSON builders
    // -------------------------------------------------------------------------

    private fun buildRegisterJson(
        challenge: ByteArray,
        userId: ByteArray,
        accountName: String,
    ): String = JSONObject().apply {
        put("challenge", challenge.b64())
        put("rp", JSONObject().apply {
            put("id", RP_ID)
            put("name", RP_NAME)
        })
        put("user", JSONObject().apply {
            put("id", userId.b64())
            put("name", accountName)
            put("displayName", accountName)
        })
        put("pubKeyCredParams", JSONArray().apply {
            put(JSONObject().apply { put("type", "public-key"); put("alg", -7) })
            put(JSONObject().apply { put("type", "public-key"); put("alg", -257) })
        })
        put("authenticatorSelection", JSONObject().apply {
            put("authenticatorAttachment", "platform")
            put("requireResidentKey", true)
            put("residentKey", "required")
            put("userVerification", "required")
        })
        put("extensions", JSONObject().apply {
            put("prf", JSONObject().apply {
                put("eval", JSONObject().apply {
                    put("first", PRF_SALT_BYTES.b64())
                })
            })
        })
    }.toString()

    private fun buildAssertJson(challenge: ByteArray, credentialId: String?): String =
        JSONObject().apply {
            put("challenge", challenge.b64())
            put("rpId", RP_ID)
            if (credentialId != null) {
                put("allowCredentials", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "public-key")
                        put("id", credentialId)
                    })
                })
            }
            put("userVerification", "required")
            put("extensions", JSONObject().apply {
                put("prf", JSONObject().apply {
                    put("eval", JSONObject().apply {
                        put("first", PRF_SALT_BYTES.b64())
                    })
                })
            })
        }.toString()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun randomBytes(size: Int) = ByteArray(size).also { SecureRandom().nextBytes(it) }

    private fun ByteArray.b64() = Base64.encodeToString(this, B64_FLAGS)

    private fun parsePrf(responseJson: String): ByteArray? {
        val first = JSONObject(responseJson)
            .optJSONObject("clientExtensionResults")
            ?.optJSONObject("prf")
            ?.optJSONObject("results")
            ?.optString("first")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return Base64.decode(first, B64_FLAGS)
    }

    // userId format: "$accountName::$uuid" as UTF-8 bytes
    // The UUID ensures uniqueness so two wallets with the same name don't overwrite each other.
    // WebAuthn requires user.id ≤ 64 bytes; "::$uuid" occupies 38 bytes, leaving 26 for the name.
    private fun encodeUserId(accountName: String): ByteArray {
        val nameBytes = accountName.toByteArray(Charsets.UTF_8).let {
            if (it.size <= 26) it else it.copyOf(26)
        }
        val suffix = "::${UUID.randomUUID()}".toByteArray(Charsets.UTF_8)
        return nameBytes + suffix
    }

    private fun decodeAccountName(assertionResponseJson: String): String? {
        return try {
            val userHandle = JSONObject(assertionResponseJson)
                .optJSONObject("response")
                ?.optString("userHandle")
                ?.takeIf { it.isNotBlank() }
                ?: return null
            val userId = String(Base64.decode(userHandle, B64_FLAGS), Charsets.UTF_8)
            userId.substringBefore("::").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
