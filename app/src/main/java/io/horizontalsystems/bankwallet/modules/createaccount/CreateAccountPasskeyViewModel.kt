package io.horizontalsystems.bankwallet.modules.createaccount

import android.app.Activity
import android.util.Base64
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.normalizeNFKD
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.SecureRandom

class CreateAccountPasskeyViewModel(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
) : ViewModelUiState<CreateAccountPasskeyUiState>() {
    private val defaultAccountName = accountFactory.getNextAccountName()
    private var accountName: String = defaultAccountName
    private var success: AccountType? = null
    private var error: String? = null

    override fun createState() = CreateAccountPasskeyUiState(
        defaultAccountName = defaultAccountName,
        success = success,
        error = error,
    )
    fun createAccount(activity: Activity) {
        viewModelScope.launch {
            try {
                val entropy = registerPasskeyWithPrf(activity)
                val words = Mnemonic().toMnemonic(entropy, Language.English)
                    .map { it.normalizeNFKD() }
                val accountType = AccountType.Mnemonic(words, "")
                val account = accountFactory.account(
                    accountName,
                    accountType,
                    AccountOrigin.Created,
                    false,
                    false,
                )
                accountManager.save(account)
                activateDefaultWallets(account)
                success = accountType
                error = null
            } catch (e: CreatePublicKeyCredentialException) {
                error = e.message
                success = null
            } catch (e: Exception) {
                error = e.message
                success = null
            }
            emitState()
        }
    }

    fun onChangeAccountName(v: String) {
        accountName = v.ifBlank { defaultAccountName }
        emitState()
    }

    fun onErrorDisplayed() {
        error = null
        emitState()
    }

    private suspend fun registerPasskeyWithPrf(activity: Activity): ByteArray {
        val credentialManager = CredentialManager.create(activity)
        // Fixed app-level salt — same passkey + same salt always produces the same entropy
        val prfSalt = "unstoppable-wallet-v1".toByteArray(Charsets.UTF_8)

        // Step 1: register the passkey (PRF is enabled but output is not returned on create)
        val registerChallenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val userId = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val registerResponse = credentialManager.createCredential(
            context = activity,
            request = CreatePublicKeyCredentialRequest(
                requestJson = buildRegisterJson(registerChallenge, userId, prfSalt)
            ),
        ) as CreatePublicKeyCredentialResponse

        val credentialId = JSONObject(registerResponse.registrationResponseJson).getString("id")

        // Step 2: assert immediately with PRF eval to get the deterministic entropy
        val assertChallenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val assertResult = credentialManager.getCredential(
            context = activity,
            request = GetCredentialRequest(
                listOf(
                    GetPublicKeyCredentialOption(
                        requestJson = buildAssertJson(assertChallenge, credentialId, prfSalt)
                    )
                )
            ),
        )
        val assertionJson = (assertResult.credential as PublicKeyCredential).authenticationResponseJson

        return parsePrfOutput(assertionJson)
    }

    private fun buildRegisterJson(
        challenge: ByteArray,
        userId: ByteArray,
        prfSalt: ByteArray,
    ): String {
        val b64 = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val challengeB64 = Base64.encodeToString(challenge, b64)
        val userIdB64 = Base64.encodeToString(userId, b64)
        val prfSaltB64 = Base64.encodeToString(prfSalt, b64)
        val name = JSONObject.quote(accountName)

        return """
            {
              "challenge": "$challengeB64",
              "rp": {
                "id": "unstoppable.wallet",
                "name": "Unstoppable Wallet"
              },
              "user": {
                "id": "$userIdB64",
                "name": $name,
                "displayName": $name
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
        credentialId: String,
        prfSalt: ByteArray,
    ): String {
        val b64 = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val challengeB64 = Base64.encodeToString(challenge, b64)
        val prfSaltB64 = Base64.encodeToString(prfSalt, b64)

        return """
            {
              "challenge": "$challengeB64",
              "rpId": "unstoppable.wallet",
              "allowCredentials": [
                {"type": "public-key", "id": "$credentialId"}
              ],
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

    private fun parsePrfOutput(assertionResponseJson: String): ByteArray {
        val root = JSONObject(assertionResponseJson)
        val prfResults = root
            .getJSONObject("clientExtensionResults")
            .getJSONObject("prf")
            .getJSONObject("results")
            .getString("first")
        return Base64.decode(prfResults, Base64.URL_SAFE or Base64.NO_PADDING)
    }

    private fun activateDefaultWallets(account: Account) {
        val tokenQueries = listOf(
            TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)),
            TokenQuery(BlockchainType.Ethereum, TokenType.Native),
            TokenQuery(BlockchainType.Monero, TokenType.Native),
            TokenQuery(BlockchainType.Tron, TokenType.Native),
            TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Native),
            TokenQuery(BlockchainType.Tron, TokenType.Eip20("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")),
            TokenQuery(BlockchainType.Ethereum, TokenType.Eip20("0xdac17f958d2ee523a2206206994597c13d831ec7")),
        )
        walletActivator.activateWallets(account, tokenQueries)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateAccountPasskeyViewModel(
                App.accountFactory,
                App.accountManager,
                App.walletActivator,
            ) as T
        }
    }
}

data class CreateAccountPasskeyUiState(
    val defaultAccountName: String,
    val success: AccountType? = null,
    val error: String? = null,
)
