package io.horizontalsystems.bankwallet.modules.walletconnect.stellar

import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.bankwallet.modules.walletconnect.request.AbstractWCAction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCActionContentItem
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCActionState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WCActionStellarSignAndSubmitXdr(
    private val paramsJsonStr: String,
    private val peerName: String,
) : AbstractWCAction() {

    private val gson = GsonBuilder().create()
    private val params = gson.fromJson(paramsJsonStr, Params::class.java)

    private val token = App.marketKit.token(TokenQuery(BlockchainType.Stellar, TokenType.Native))!!
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
            sendTransactionService.setSendTransactionData(SendTransactionData.Stellar.WithTransactionEnvelope(params.xdr))
        }
    }

    override fun getTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SendTransactionRequest_Title)
    }

    override fun getApproveButtonTitle(): TranslatableString {
        return TranslatableString.ResString(R.string.WalletConnect_SendTransactionRequest_ButtonSend)
    }

    override suspend fun performAction(): String {
        sendTransactionService.sendTransaction()

        return gson.toJson(mapOf("status" to "success"))
    }

    override fun createState() = WCActionState(
        runnable = sendTransactionState.sendable,
        items = listOf(
            WCActionContentItem.Section(
                listOf(
                    WCActionContentItem.SingleLine(
                        TranslatableString.ResString(R.string.WalletConnect_SignMessageRequest_dApp),
                        TranslatableString.PlainString(peerName)
                    ),
                    WCActionContentItem.SingleLine(
                        TranslatableString.PlainString("Stellar"),
                        null
                    )
                )
            ),

            WCActionContentItem.Section(
                listOf(
                    WCActionContentItem.Paragraph(
                        TranslatableString.PlainString(params.xdr)
                    )
                ),
                TranslatableString.PlainString("Transaction XDR")
            ),

            WCActionContentItem.Fee(
                sendTransactionState.networkFee
            )
        )
    )

    data class Params(val xdr: String)
}

