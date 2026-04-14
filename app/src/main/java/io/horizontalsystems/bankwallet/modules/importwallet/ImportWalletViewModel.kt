package io.horizontalsystems.bankwallet.modules.importwallet

import android.app.Activity
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
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

class ImportWalletViewModel(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
) : ViewModelUiState<ImportWalletUiState>() {

    private var success: AccountType? = null
    private var error: String? = null

    override fun createState() = ImportWalletUiState(
        success = success,
        error = error,
    )

    fun restoreFromPasskey(activity: Activity) {
        viewModelScope.launch {
            try {
                val (entropy, accountName) = authenticatePasskeyWithPrf(activity)
                val words = Mnemonic().toMnemonic(entropy, Language.English)
                    .map { it.normalizeNFKD() }
                val accountType = AccountType.Mnemonic(words, "")
                val account = accountFactory.account(
                    name = accountName,
                    type = accountType,
                    origin = AccountOrigin.Restored,
                    backedUp = true,
                    fileBackedUp = false,
                )
                accountManager.save(account)
                activateDefaultWallets(account)
                success = accountType
                error = null
            } catch (e: Exception) {
                error = e.message
                success = null
            }
            emitState()
        }
    }

    fun onErrorDisplayed() {
        error = null
        emitState()
    }

    private suspend fun authenticatePasskeyWithPrf(activity: Activity): Pair<ByteArray, String> {
        val credentialManager = CredentialManager.create(activity)
        val prfSalt = "unstoppable-wallet-v1".toByteArray(Charsets.UTF_8)
        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }

        val result = credentialManager.getCredential(
            context = activity,
            request = GetCredentialRequest(
                listOf(
                    GetPublicKeyCredentialOption(
                        requestJson = buildAssertJson(challenge, prfSalt)
                    )
                )
            ),
        )
        val assertionJson = (result.credential as PublicKeyCredential).authenticationResponseJson
        val entropy = parsePrfOutput(assertionJson)
        val accountName = parseAccountName(assertionJson) ?: accountFactory.getNextAccountName()
        return Pair(entropy, accountName)
    }

    private fun buildAssertJson(challenge: ByteArray, prfSalt: ByteArray): String {
        val b64 = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val challengeB64 = Base64.encodeToString(challenge, b64)
        val prfSaltB64 = Base64.encodeToString(prfSalt, b64)

        return """
            {
              "challenge": "$challengeB64",
              "rpId": "unstoppable.money",
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

    private fun parseAccountName(assertionResponseJson: String): String? {
        return try {
            JSONObject(assertionResponseJson)
                .optJSONObject("user")
                ?.optString("name")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
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
            return ImportWalletViewModel(
                App.accountFactory,
                App.accountManager,
                App.walletActivator,
            ) as T
        }
    }
}

data class ImportWalletUiState(
    val success: AccountType? = null,
    val error: String? = null,
)
