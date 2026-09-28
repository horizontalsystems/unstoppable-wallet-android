package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.transaction.TransactionSender
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendNearAdapter
import io.horizontalsystems.walletkit.core.LocalizedException
import io.horizontalsystems.walletkit.core.NearSendEstimate
import io.horizontalsystems.walletkit.core.adapters.BaseNearAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

/**
 * Sends NEAR or a NEP-141 token: from the send screen, as the deposit leg of a swap, or for an
 * OpenCryptoPay payment.
 *
 * The fee is only known once the receiver is: a transfer to an implicit account pays for
 * creating it, and a token send to an unregistered account also pays its storage deposit. Both
 * come out of the NEAR balance, which must also cover gas bought up front at a price above the
 * one finally charged, so the check is against the estimate's required balance.
 */
class SendTransactionServiceNear(
    private val token: Token,
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Near())

    private val adapter = App.adapterManager.getAdapterForToken<ISendNearAdapter>(token)
        ?: throw IllegalStateException("No NEAR adapter for ${token.coin.code}")

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Near, TokenType.Native))
        ?: throw IllegalArgumentException("NEAR native token not found for fee calculation")

    private var sendData: SendTransactionData.Near? = null
    private var estimate: NearSendEstimate? = null
    private var cautions: List<CautionViewItem> = emptyList()

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Near)

        var newEstimate: NearSendEstimate? = null
        val newCautions = mutableListOf<CautionViewItem>()
        try {
            newEstimate = withContext(Dispatchers.IO) { adapter.estimate(data.address, data.amount, data.memo) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TransactionSender.SendError.StorageDepositTooHigh) {
            newCautions.add(storageDepositTooHigh())
        } catch (e: Throwable) {
            // without an estimate the balance cannot be checked, so the send stays blocked
            newCautions.add(CautionViewItem.fromThrowable(e))
        }

        if (adapter.pendingTransactionHashes.isNotEmpty()) {
            newCautions.add(
                CautionViewItem(
                    title = Translator.getString(R.string.Alert_TitleWarning),
                    text = Translator.getString(R.string.NearSend_PreviousTransactionPending),
                    type = CautionViewItem.Type.Warning
                )
            )
        }

        if (data.amount > adapter.maxSendableBalance && token.type != TokenType.Native) {
            newCautions.add(insufficientBalance())
        }
        val availableNear = (adapter as? BaseNearAdapter)?.availableNear
        if (newEstimate != null && availableNear != null && availableNear < newEstimate.requiredNear) {
            newCautions.add(insufficientBalance())
        }

        // publish together so state never pairs new data with old cautions
        sendData = data
        estimate = newEstimate
        cautions = newCautions.distinctBy { it.title to it.text }

        emitState()
    }

    private fun insufficientBalance() = CautionViewItem(
        title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
        text = Translator.getString(R.string.Swap_ErrorInsufficientBalance),
        type = CautionViewItem.Type.Error
    )

    private fun storageDepositTooHigh() = CautionViewItem(
        title = Translator.getString(R.string.Alert_TitleWarning),
        text = Translator.getString(R.string.NearSend_StorageDepositTooHigh),
        type = CautionViewItem.Type.Error
    )

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val data = sendData ?: throw IllegalStateException("Send data not set")
        val estimate = estimate ?: throw IllegalStateException("Estimate not set")

        val pendingBefore = adapter.pendingTransactionHashes
        val txHash = try {
            withContext(Dispatchers.IO) {
                adapter.send(data.amount, data.address, data.memo, estimate)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TransactionSender.SendError.StorageDepositChanged) {
            // show the new deposit so the next confirmation is for what will be paid
            setSendTransactionData(data)
            throw LocalizedException(R.string.NearSend_StorageDepositChanged)
        } catch (e: TransactionSender.SendError.StorageDepositTooHigh) {
            throw LocalizedException(R.string.NearSend_StorageDepositTooHigh)
        } catch (e: TransactionSender.SendError.Rejected) {
            // kept as pending when the kit cannot tell whether an earlier attempt got it on chain
            newPendingHash(pendingBefore) ?: throw rejectedError(e.details)
        } catch (e: TransactionSender.SendError) {
            throw e
        } catch (e: Exception) {
            // Past signing, the kit keeps a transaction that may have reached a node as pending,
            // and it resolves in the history; reporting a failure would invite a second send.
            // Failures before signing leave no record.
            newPendingHash(pendingBefore) ?: throw (noConnection(e) ?: e)
        }

        return SendTransactionResult.Near(txHash)
    }

    // the kit wraps it in NoEndpointAvailable once every endpoint has failed
    private fun noConnection(error: Throwable) =
        generateSequence(error) { it.cause }.filterIsInstance<UnknownHostException>().firstOrNull()

    private fun newPendingHash(pendingBefore: Set<String>) =
        (adapter.pendingTransactionHashes - pendingBefore).firstOrNull()

    // details is the node's InvalidTxError, e.g. {"TxExecutionError":{"InvalidTxError":{"NotEnoughBalance":…}}}
    private fun rejectedError(details: String) = when {
        "NotEnoughBalance" in details || "LackBalanceForState" in details ->
            LocalizedException(R.string.EthereumTransaction_Error_InsufficientBalance_Title)

        else -> LocalizedException(R.string.NearSend_Rejected)
    }

    // Before the receiver is known, the most a NEAR transfer to any receiver can move.
    override fun maxSendableAmount(): BigDecimal? =
        if (token.type == TokenType.Native) adapter.maxSendableBalance else null

    override fun createState(): SendTransactionServiceState {
        val estimate = estimate
        return SendTransactionServiceState(
            uuid = uuid,
            networkFee = getAmountData(CoinValue(feeToken, estimate?.fee ?: adapter.fee)),
            cautions = cautions,
            sendable = sendData != null && estimate != null && cautions.none { it.type == CautionViewItem.Type.Error },
            loading = sendData == null,
            fields = listOfNotNull(
                estimate?.storageDeposit?.let {
                    DataFieldNearReceiverRegistration(App.numberFormatter.formatCoinFull(it, feeToken.coin.code, feeToken.decimals))
                }
            ),
        )
    }
}
