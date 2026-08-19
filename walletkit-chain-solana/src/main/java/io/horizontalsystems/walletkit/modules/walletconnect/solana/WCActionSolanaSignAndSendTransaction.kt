package io.horizontalsystems.walletkit.modules.walletconnect.solana

import android.util.Base64
import com.google.gson.GsonBuilder
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ValueType
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.walletkit.modules.walletconnect.request.WCActionState
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

// Handles `solana_signAndSendTransaction`. Params: { transaction: <base64>, sendOptions? }.
// The transaction is signed and broadcast by SolanaKit (via the raw-transaction send path).
// Response: { signature: <base58 txid> }.
class WCActionSolanaSignAndSendTransaction(
    private val paramsJsonStr: String,
    private val peerName: String?,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()
    private val params = gson.fromJson(paramsJsonStr, Params::class.java)

    // Bound the untrusted payload before decoding: a Solana transaction is at most ~1644 base64
    // chars, so anything larger is rejected up front rather than driving a large decode/send (a
    // malicious dApp otherwise could exhaust CPU/heap). Thrown here, this is caught by
    // WCManager.getActionForRequest and surfaced as an error screen.
    private val rawTransaction = run {
        require(paramsJsonStr.length <= WCSolanaHelper.maxParamsJsonLength) {
            "WalletConnect request is too large"
        }
        require(params.transaction.length <= WCSolanaHelper.maxTransactionBase64Length) {
            "WalletConnect transaction is too large"
        }
        Base64.decode(params.transaction, Base64.NO_WRAP)
    }

    private val token = App.marketKit.token(TokenQuery(BlockchainType.Solana, TokenType.Native))
        ?: throw IllegalStateException("Native Solana token not found")
    private val sendTransactionService = SendTransactionServiceFactory.create(token)
    private var sendTransactionState = sendTransactionService.stateFlow.value

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                // A failed emission must not tear down the collector or the sibling coroutine
                // (an uncaught throw here would propagate to viewModelScope and crash the app).
                try {
                    sendTransactionState = transactionState
                    emitState()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Handling Solana send-transaction state failed")
                }
            }
        }

        coroutineScope.launch {
            // estimateFee (inside setSendTransactionData) can throw on RPC/decode failure; catch it
            // so the failure is logged rather than crashing the app via viewModelScope.
            try {
                sendTransactionService.setSendTransactionData(
                    SendTransactionData.Solana.WithRawTransaction(rawTransaction)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Preparing Solana send-transaction data failed")
            }
        }
    }

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SendTransactionRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SendTransactionRequest_ButtonSend)
    }

    override suspend fun performAction(): String {
        val result = sendTransactionService.sendTransaction()
        val txHash = (result as? SendTransactionResult.Solana)?.txHash
            ?: throw IllegalStateException("No transaction hash returned")

        return gson.toJson(mapOf("signature" to txHash))
    }

    override fun createState(): WCActionState {
        // Decoded summary (method, network, any directly decodable transfers). Falls back to
        // nothing on parse failure.
        val summary = WCSolanaTxSummary.summary(rawTransaction, peerName)

        // When nothing material could be decoded the user is blind-broadcasting; warn prominently.
        // We do not hard-block: many legitimate transactions (arbitrary programs, v0/lookup-table)
        // are simply beyond this best-effort decoder, and sendability is still gated by fee
        // estimation below.
        var sectionViewItems = if (summary.opaque) {
            listOf(WCSolanaTxSummary.opaqueWarningSection()) + summary.sections
        } else {
            summary.sections
        }

        sendTransactionState.networkFee?.let { networkFee ->
            sectionViewItems += SectionViewItem(
                listOf(ViewItem.Fee(networkFee))
            )
        }

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
            runnable = sendTransactionState.sendable,
            items = sectionViewItems
        )
    }

    data class Params(val transaction: String)
}
