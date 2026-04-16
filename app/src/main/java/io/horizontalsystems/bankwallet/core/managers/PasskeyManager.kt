package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Base64
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import org.json.JSONObject
import java.security.SecureRandom

class PasskeyManager {

    companion object {
        private const val RP_ID = "unstoppable.money"
        private const val RP_NAME = "Unstoppable Wallet"
        private const val PRF_SALT = "wallet"
        private val B64_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    }

    suspend fun register(context: Context, accountName: String): ByteArray {
        val credentialManager = CredentialManager.create(context)
        val prfSalt = PRF_SALT.toByteArray(Charsets.UTF_8)

        // Step 1: register the passkey. PRF output is not returned on create — only on assertion.
        val registerChallenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
        // Encode account name into userId so it can be recovered from userHandle in future assertions.
        // Format: [nameLen (1 byte)][name bytes (≤55)][random nonce (8 bytes)] = 64 bytes max.
        // The nonce ensures uniqueness so two wallets with the same name don't overwrite each other.
        val userId = run {
            val nameBytes = accountName.toByteArray(Charsets.UTF_8).let {
                if (it.size <= 55) it else it.copyOf(55)
            }
            val nonce = ByteArray(8).also { SecureRandom().nextBytes(it) }
            byteArrayOf(nameBytes.size.toByte()) + nameBytes + nonce
        }
        val registerResponse = credentialManager.createCredential(
            context = context,
            request = CreatePublicKeyCredentialRequest(
                requestJson = buildRegisterJson(registerChallenge, userId, prfSalt, accountName)
            ),
        ) as CreatePublicKeyCredentialResponse

        val registrationJson = registerResponse.registrationResponseJson

        // Some authenticators (e.g. Google Password Manager) return PRF results on create.
        // Use them directly to avoid a second getCredential call, which would fail because
        // the newly created passkey may not yet be locally available for assertion.
        parsePrfOutputOrNull(registrationJson)?.let { return it }

        // Step 2: assert immediately with PRF eval to get the deterministic entropy.
        // Used by authenticators (e.g. Samsung Pass) that only return PRF on assertion.
        val credentialId = JSONObject(registrationJson).getString("id")
        val assertChallenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val assertResult = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest(
                listOf(
                    GetPublicKeyCredentialOption(
                        requestJson = buildAssertJson(assertChallenge, prfSalt, credentialId)
                    )
                )
            ),
        )
        val assertionJson = (assertResult.credential as PublicKeyCredential).authenticationResponseJson
        return parsePrfOutput(assertionJson)
    }

    suspend fun authenticate(context: Context): Pair<ByteArray, String?> {
        val credentialManager = CredentialManager.create(context)
        val prfSalt = PRF_SALT.toByteArray(Charsets.UTF_8)
        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }

        val result = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest(
                listOf(
                    GetPublicKeyCredentialOption(
                        requestJson = buildAssertJson(challenge, prfSalt, credentialId = null)
                    )
                )
            ),
        )
        val assertionJson = (result.credential as PublicKeyCredential).authenticationResponseJson
        val entropy = parsePrfOutput(assertionJson)
        val accountName = parseAccountName(assertionJson)
        return Pair(entropy, accountName)
    }

    // -------------------------------------------------------------------------
    // JSON builders
    // -------------------------------------------------------------------------

    private fun buildRegisterJson(
        challenge: ByteArray,
        userId: ByteArray,
        prfSalt: ByteArray,
        accountName: String,
    ): String {
        val challengeB64 = Base64.encodeToString(challenge, B64_FLAGS)
        val userIdB64 = Base64.encodeToString(userId, B64_FLAGS)
        val prfSaltB64 = Base64.encodeToString(prfSalt, B64_FLAGS)
        val nameJson = JSONObject.quote(accountName)

        return """
            {
              "challenge": "$challengeB64",
              "rp": {
                "id": "$RP_ID",
                "name": "$RP_NAME"
              },
              "user": {
                "id": "$userIdB64",
                "name": $nameJson,
                "displayName": $nameJson
              },
              "pubKeyCredParams": [
                {"type": "public-key", "alg": -7},
                {"type": "public-key", "alg": -257}
              ],
              "authenticatorSelection": {
                "authenticatorAttachment": "platform",
                "requireResidentKey": true,
                "residentKey": "required",
                "userVerification": "required"
              },
              "extensions": {
                "prf": {
                  "eval": {
                    "first": "$prfSaltB64"
                  }
                }
              }
            }
        """.trimIndent()
    }

    private fun buildAssertJson(
        challenge: ByteArray,
        prfSalt: ByteArray,
        credentialId: String?,
    ): String {
        val challengeB64 = Base64.encodeToString(challenge, B64_FLAGS)
        val prfSaltB64 = Base64.encodeToString(prfSalt, B64_FLAGS)

        val allowCredentials = if (credentialId != null) {
            """"allowCredentials": [{"type": "public-key", "id": "$credentialId"}],"""
        } else {
            ""
        }

        return """
            {
              "challenge": "$challengeB64",
              "rpId": "$RP_ID",
              $allowCredentials
              "userVerification": "required",
              "extensions": {
                "prf": {
                  "eval": {
                    "first": "$prfSaltB64"
                  }
                }
              }
            }
        """.trimIndent()
    }

    // -------------------------------------------------------------------------
    // Response parsers
    // -------------------------------------------------------------------------

    private fun parsePrfOutput(responseJson: String): ByteArray {
        val root = JSONObject(responseJson)
        val prfResults = root
            .getJSONObject("clientExtensionResults")
            .getJSONObject("prf")
            .getJSONObject("results")
            .getString("first")
        return Base64.decode(prfResults, Base64.URL_SAFE or Base64.NO_PADDING)
    }

    private fun parsePrfOutputOrNull(responseJson: String): ByteArray? {
        return try {
            val first = JSONObject(responseJson)
                .optJSONObject("clientExtensionResults")
                ?.optJSONObject("prf")
                ?.optJSONObject("results")
                ?.optString("first")
                ?.takeIf { it.isNotBlank() }
                ?: return null
            Base64.decode(first, Base64.URL_SAFE or Base64.NO_PADDING)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAccountName(assertionResponseJson: String): String? {
        return try {
            val userHandle = JSONObject(assertionResponseJson)
                .optJSONObject("response")
                ?.optString("userHandle")
                ?.takeIf { it.isNotBlank() }
                ?: return null
            val bytes = Base64.decode(userHandle, B64_FLAGS)
            // Format written during registration: [nameLen][name bytes][nonce]
            val nameLen = bytes[0].toInt() and 0xFF
            String(bytes, 1, nameLen, Charsets.UTF_8)
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
