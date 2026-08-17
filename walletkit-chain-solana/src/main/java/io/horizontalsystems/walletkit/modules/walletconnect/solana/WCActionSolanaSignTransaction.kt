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
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()

    private val base64Transactions: List<String> = if (multiple) {
        gson.fromJson(paramsJsonStr, MultiParams::class.java).transactions
    } else {
        listOf(gson.fromJson(paramsJsonStr, SingleParams::class.java).transaction)
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
            val first = signed.first()
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
        val rows = mutableListOf<ViewItem>(
            ViewItem.Value(
                Translator.getString(R.string.WalletConnect_SignMessageRequest_Title),
                if (multiple) {
                    Translator.getString(
                        R.string.WalletConnect_Solana_SignTransactionsCount,
                        base64Transactions.size
                    )
                } else {
                    Translator.getString(R.string.WalletConnect_Solana_SignTransaction)
                },
                ValueType.Regular
            )
        )

        App.accountManager.activeAccount?.name?.let { walletName ->
            rows.add(
                ViewItem.Value(
                    Translator.getString(R.string.Wallet_Title),
                    walletName,
                    ValueType.Regular
                )
            )
        }

        return WCActionState(
            runnable = true,
            items = listOf(SectionViewItem(rows))
        )
    }

    private data class SignedTransaction(val base64: String, val signatureBase58: String)

    private data class SingleParams(val transaction: String)
    private data class MultiParams(val transactions: List<String>)
}
