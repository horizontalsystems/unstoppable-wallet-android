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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Handles `solana_signAndSendTransaction`. Params: { transaction: <base64>, sendOptions? }.
// The transaction is signed and broadcast by SolanaKit (via the raw-transaction send path).
// Response: { signature: <base58 txid> }.
class WCActionSolanaSignAndSendTransaction(
    private val paramsJsonStr: String,
    private val peerName: String?,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()
    private val params = gson.fromJson(paramsJsonStr, Params::class.java)
    private val rawTransaction = Base64.decode(params.transaction, Base64.NO_WRAP)

    private val token = App.marketKit.token(TokenQuery(BlockchainType.Solana, TokenType.Native))!!
    private val sendTransactionService = SendTransactionServiceFactory.create(token)
    private var sendTransactionState = sendTransactionService.stateFlow.value

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState

                emitState()
            }
        }

        coroutineScope.launch {
            sendTransactionService.setSendTransactionData(
                SendTransactionData.Solana.WithRawTransaction(rawTransaction)
            )
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
        var sectionViewItems = WCSolanaTxSummary.sections(rawTransaction, peerName)

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
