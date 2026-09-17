package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import java.math.BigDecimal
import io.horizontalsystems.walletkit.modules.send.SendErrorMinimumSendAmount
import io.horizontalsystems.walletkit.core.ISendXrpAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldDestinationTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Sends the deposit leg of a swap (or an OpenCryptoPay payment) on the XRP Ledger.
 *
 * Two things are XRP-specific and both can cost the user the deposit: the counterparty's
 * crediting identifier is a [SendTransactionData.Xrp.destinationTag], a field of the Payment
 * rather than a memo; and the spendable balance stops short of the account's reserve, so the
 * amount is checked against what the adapter will actually let go, not the raw balance.
 */
class SendTransactionServiceXrp(
    private val token: Token,
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Xrp())

    private val adapter = App.adapterManager.getAdapterForToken<ISendXrpAdapter>(token)
        ?: throw IllegalStateException("No XRP adapter for ${token.coin.code}")

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Xrp, TokenType.Native))
        ?: throw IllegalArgumentException("XRP native token not found for fee calculation")

    private var sendData: SendTransactionData.Xrp? = null
    private var cautions: List<CautionViewItem> = emptyList()

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Xrp)

        // build first, then publish both together so state never pairs new data with old cautions
        val newCautions = buildCautions(data)
        sendData = data
        cautions = newCautions

        emitState()
    }

    private suspend fun buildCautions(data: SendTransactionData.Xrp): List<CautionViewItem> {
        val result = mutableListOf<CautionViewItem>()

        if (data.amount > adapter.maxSendableBalance) {
            result.add(
                CautionViewItem(
                    title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                    text = Translator.getString(R.string.Swap_ErrorInsufficientBalance),
                    type = CautionViewItem.Type.Error
                )
            )
        }

        // A destination not yet on the ledger is created by the payment, which the network
        // accepts only from the base reserve upwards; below that the send would fail on
        // broadcast, so it is refused here. A failed lookup blocks the send too.
        if (token.type == TokenType.Native) {
            try {
                val minimum = adapter.getMinimumSendAmount(data.address)
                if (minimum != null && data.amount < minimum) {
                    result.add(SendErrorMinimumSendAmount(minimum, token.coin.code).toCautionViewItem())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                result.add(CautionViewItem.fromThrowable(e))
            }
        }

        // An untagged payment to an address flagged RequireDestTag is rejected by the ledger,
        // and the route would have to be rebuilt anyway — so refuse before the fee is burned.
        // A failed lookup blocks the send too: an unknown flag is not the same as no flag.
        if (data.destinationTag == null) {
            try {
                if (adapter.requiresDestinationTag(data.address)) {
                    result.add(
                        CautionViewItem(
                            title = Translator.getString(R.string.Send_DestinationTag),
                            text = Translator.getString(R.string.Send_DestinationTag_Required),
                            type = CautionViewItem.Type.Error
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                result.add(
                    CautionViewItem(
                        title = Translator.getString(R.string.Send_DestinationTag),
                        text = e.message ?: Translator.getString(R.string.SyncError),
                        type = CautionViewItem.Type.Error
                    )
                )
            }
        }

        return result
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val data = sendData ?: throw IllegalStateException("Send data not set")

        val txHash = withContext(Dispatchers.IO) {
            adapter.send(data.amount, data.address, data.destinationTag, null)
        }

        return SendTransactionResult.Xrp(txHash)
    }

    // The send screen already offers the balance net of the fee and reserve for XRP, so the
    // maximum is that figure itself.
    override fun maxSendableAmount(): BigDecimal? =
        if (token.type == TokenType.Native) adapter.maxSendableBalance.coerceAtLeast(BigDecimal.ZERO) else null

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = getAmountData(CoinValue(feeToken, adapter.fee)),
        cautions = cautions,
        sendable = sendData != null && cautions.none { it.type == CautionViewItem.Type.Error },
        loading = sendData == null,
        fields = listOfNotNull(sendData?.destinationTag?.let { DataFieldDestinationTag(it) }),
    )
}
