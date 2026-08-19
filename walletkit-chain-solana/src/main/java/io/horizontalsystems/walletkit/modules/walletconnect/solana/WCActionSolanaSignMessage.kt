package io.horizontalsystems.walletkit.modules.walletconnect.solana

import com.google.gson.GsonBuilder
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ValueType
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.walletkit.modules.walletconnect.request.WCActionState
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.solanakit.Signer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

// Handles `solana_signMessage`. Params: { message: <base58>, pubkey: <base58> }.
// Response: { signature: <base58> }.
//
// SECURITY: `solana_signMessage` and `solana_signTransaction` are the SAME ed25519 operation over
// raw bytes — Signer.signMessage does account.sign(bytes), Signer.signTransaction does
// account.sign(message.serialize()) — with no domain separation. So a signature over an
// attacker-chosen "message" that happens to BE a serialized Solana transaction message is a valid
// transaction signature the dApp can broadcast, draining the wallet. The sign screen would only
// show that payload as text, hiding the real recipient, amount and fee. To close this oracle we
// refuse any payload the signer flags as a serialized transaction message
// (Signer.isTransactionMessage) and any payload whose `pubkey` names an account other than the
// active wallet. Genuine off-chain messages are unaffected.
class WCActionSolanaSignMessage(
    private val paramsJsonStr: String,
    private val signer: Signer,
    // Base58 address of the active (signing) wallet, used to reject requests whose `pubkey` names a
    // different account. Null when it can't be resolved; a request that DOES supply a `pubkey` is
    // then failed closed (refused), since we can't verify it matches this wallet.
    private val expectedPubkey: String?,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()

    // Bound the raw JSON BEFORE gson parses it, so a giant payload can't exhaust the heap during
    // deserialization. Runs before the `params` initializer below (Kotlin runs initializers
    // top-to-bottom). Thrown here, it is caught by WCManager.getActionForRequest.
    init {
        require(paramsJsonStr.length <= WCSolanaHelper.maxParamsJsonLength) {
            "WalletConnect request is too large"
        }
    }

    private val params = gson.fromJson(paramsJsonStr, Params::class.java)

    // Bound the untrusted message before base58-decoding it (and before the transaction-message
    // probe deserializes it): a malicious dApp must not be able to hand us a huge string and drive a
    // large decode/parse. Off-chain messages are small; this ceiling is very generous. Thrown here,
    // it is caught by WCManager.getActionForRequest and surfaced as an error screen.
    private val messageBytes = run {
        require(params.message.length <= WCSolanaHelper.maxParamsJsonLength) {
            "WalletConnect message is too large"
        }
        WCSolanaHelper.base58Decode(params.message)
    }

    // Non-null string resource when the request MUST be refused. Computed once so createState()
    // (disable approve + explain) and performAction() (hard gate) can never disagree. The
    // transaction-message check lives in the signer (which owns the sol4k codec it signs with), so
    // detection matches the exact bytes that would be signed.
    private val rejectionReasonRes: Int? = when {
        signer.isTransactionMessage(messageBytes) -> R.string.WalletConnect_Solana_SignMessage_LooksLikeTransaction
        pubkeyMismatch() -> R.string.WalletConnect_Solana_SignMessage_ForeignPubkey
        else -> null
    }

    // A supplied `pubkey` that doesn't match the active wallet is a request meant for another
    // account; refuse it. If the dApp omits `pubkey` there is nothing to check (allow). But if it
    // DOES supply one and we couldn't resolve the active address (expectedPubkey == null), fail
    // closed and refuse rather than sign for a pubkey we can't verify.
    private fun pubkeyMismatch(): Boolean {
        val requested = params.pubkey ?: return false
        return requested != expectedPubkey
    }

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.Button_Sign)
    }

    override suspend fun performAction(): String {
        // Hard gate: createState() already disables approve for a refused request, but never sign
        // refused bytes even if performAction() is somehow reached.
        rejectionReasonRes?.let { throw ForbiddenMessageException(Translator.getString(it)) }

        // Signing is a cryptographic operation; run it off the caller's dispatcher (approve() uses
        // Dispatchers.Default) on Dispatchers.IO per the crypto-on-IO coding guideline.
        val signature = withContext(Dispatchers.IO) { signer.signMessage(messageBytes) }
        return gson.toJson(mapOf("signature" to WCSolanaHelper.base58Encode(signature)))
    }

    override fun start(coroutineScope: CoroutineScope) = Unit

    override fun createState(): WCActionState {
        val sectionViewItems = mutableListOf<SectionViewItem>()

        rejectionReasonRes?.let { reasonRes ->
            sectionViewItems += SectionViewItem(
                listOf(
                    ViewItem.Alert(
                        title = Translator.getString(R.string.WalletConnect_Solana_SignMessage_Warning),
                        text = Translator.getString(reasonRes),
                        critical = true
                    )
                )
            )
        }

        sectionViewItems += SectionViewItem(
            listOf(ViewItem.Input(Translator.getString(R.string.WalletConnect_SignMessageRequest_Title), displayMessage()))
        )

        App.accountManager.activeAccount?.name?.let { walletName ->
            sectionViewItems += SectionViewItem(
                listOf(
                    ViewItem.Value(
                        Translator.getString(R.string.Wallet_Title),
                        walletName,
                        ValueType.Regular
                    )
                )
            )
        }

        return WCActionState(
            // Approve is disabled for a refused request; the warning row above says why.
            runnable = rejectionReasonRes == null,
            items = sectionViewItems
        )
    }

    // Render the payload without loss: valid UTF-8 as text (as before), anything else as hex — so
    // non-UTF-8 bytes are neither silently mangled by U+FFFD replacement nor hidden from the user.
    private fun displayMessage(): String {
        return decodeUtf8Strict(messageBytes)
            ?: messageBytes.joinToString(separator = " ") { "%02x".format(it) }
    }

    data class Params(val message: String, val pubkey: String?)

    class ForbiddenMessageException(message: String) : Exception(message)

    companion object {
        private fun decodeUtf8Strict(bytes: ByteArray): String? {
            return try {
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
            } catch (e: Exception) {
                null
            }
        }
    }
}
