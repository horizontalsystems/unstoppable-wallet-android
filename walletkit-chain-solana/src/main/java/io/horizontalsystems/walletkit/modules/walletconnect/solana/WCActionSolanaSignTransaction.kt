package io.horizontalsystems.walletkit.modules.walletconnect.solana

import android.util.Base64
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

// Handles `solana_signTransaction` (single) and `solana_signAllTransactions` (array).
// Signs offline (via the Solana kit, which supports both legacy and versioned/V0 transactions)
// without broadcasting.
//   single   -> params { transaction: <base64> }         -> { signature: <base58>, transaction: <base64> }
//   multiple -> params { transactions: [<base64>, ...] }  -> { transactions: [<base64>, ...] }
class WCActionSolanaSignTransaction(
    private val paramsJsonStr: String,
    private val signer: Signer,
    private val multiple: Boolean,
    private val peerName: String?,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()

    // Parsed defensively: a malformed or empty request (missing/empty `transactions`, null entries)
    // must fail here with a clear error rather than crash later on `first()` / empty access. The
    // resulting list is guaranteed non-empty with no blank entries. Thrown during construction, this
    // is caught by WCManager.getActionForRequest and surfaced as an error screen.
    private val base64Transactions: List<String> = run {
        val transactions: List<String?> = if (multiple) {
            gson.fromJson(paramsJsonStr, MultiParams::class.java)?.transactions ?: emptyList()
        } else {
            listOf(gson.fromJson(paramsJsonStr, SingleParams::class.java)?.transaction)
        }
        val valid = transactions.filterNotNull().filter { it.isNotBlank() }
        require(valid.isNotEmpty()) { "WalletConnect request has no transaction to sign" }
        valid
    }

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.Button_Sign)
    }

    override suspend fun performAction(): String {
        val signed = base64Transactions.map { signAndReserialize(it) }

        return if (multiple) {
            gson.toJson(mapOf("transactions" to signed.map { it.base64 }))
        } else {
            // base64Transactions is guaranteed non-empty (validated in its initializer), but guard
            // explicitly rather than rely on that invariant from here.
            val first = signed.firstOrNull()
                ?: throw IllegalStateException("WalletConnect request has no transaction to sign")
            gson.toJson(
                mapOf(
                    "signature" to first.signatureBase58,
                    "transaction" to first.base64,
                )
            )
        }
    }

    private fun signAndReserialize(base64Transaction: String): SignedTransaction {
        val signed = signer.signTransaction(Base64.decode(base64Transaction, Base64.NO_WRAP))

        return SignedTransaction(
            base64 = Base64.encodeToString(signed.serializedTransaction, Base64.NO_WRAP),
            signatureBase58 = WCSolanaHelper.base58Encode(signed.signature),
        )
    }

    override fun start(coroutineScope: CoroutineScope) = Unit

    override fun createState(): WCActionState {
        val sections = mutableListOf<SectionViewItem>()

        // Header: for a batch (`solana_signAllTransactions`) show how many transactions are signed.
        if (multiple) {
            sections.add(
                SectionViewItem(
                    listOf(
                        ViewItem.Value(
                            Translator.getString(R.string.WalletConnect_SignMessageRequest_Title),
                            Translator.getQuantityString(R.plurals.WalletConnect_Solana_SignTransactionsCount, base64Transactions.size, base64Transactions.size),
                            ValueType.Regular
                        )
                    )
                )
            )
        }

        // Decoded summary per transaction (method, network, any directly decodable transfers).
        // Falls back to nothing on parse failure.
        base64Transactions.forEach { base64Transaction ->
            sections += WCSolanaTxSummary.sections(Base64.decode(base64Transaction, Base64.NO_WRAP), peerName)
        }

        App.accountManager.activeAccount?.name?.let { walletName ->
            sections.add(
                SectionViewItem(
                    listOf(
                        ViewItem.Value(
                            Translator.getString(R.string.Wallet_Title),
                            walletName,
                            ValueType.Regular
                        )
                    )
                )
            )
        }

        return WCActionState(
            runnable = true,
            items = sections
        )
    }

    private data class SignedTransaction(val base64: String, val signatureBase58: String)

    private data class SingleParams(val transaction: String)
    private data class MultiParams(val transactions: List<String>)
}
