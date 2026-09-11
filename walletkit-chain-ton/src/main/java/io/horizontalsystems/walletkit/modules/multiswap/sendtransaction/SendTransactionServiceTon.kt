package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendTonAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.FriendlyAddress
import io.horizontalsystems.tonkit.core.TonKit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.net.UnknownHostException

class SendTransactionServiceTon(
    private val token: Token
) : AbstractSendTransactionService(false, false) {
    private val adapter = App.adapterManager.getAdapterForToken<ISendTonAdapter>(token)!!
    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Ton, TokenType.Native))
        ?: throw IllegalArgumentException()

    lateinit var tonKit: TonKit

    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Ton())

    private var transactionType: TransactionType? = null
    private var fee: BigDecimal? = null
    // The fee comes from emulating the transfer on the network; when that fails the transfer
    // is kept and the failure shown, rather than the whole confirmation failing.
    private var feeCaution: CautionViewItem? = null

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Ton)

        fee = null
        feeCaution = null

        when (data) {
            is SendTransactionData.Ton.Regular -> {
                val address = FriendlyAddress.parse(data.address)
                transactionType = TransactionType.Regular(address, data.amount, data.memo)
                emitState()

                estimateFee { adapter.estimateFee(data.amount, address, data.memo) }
            }

            is SendTransactionData.Ton.SendRequest -> {
                val request = SendRequestEntity(
                    data = data.requestJson,
                    tonConnectRequestId = "",
                    dAppId = ""
                )

                val boc = adapter.sign(request)
                transactionType = TransactionType.Boc(boc)
                emitState()

                estimateFee { adapter.estimateFee(boc) }
            }
        }

        emitState()
    }

    // A few attempts with a short pause absorb a node hiccup; a persistent failure becomes
    // a caution that keeps the send button disabled.
    private suspend fun estimateFee(estimate: suspend () -> BigDecimal) {
        var lastError: Throwable? = null
        repeat(ESTIMATE_ATTEMPTS) { attempt ->
            try {
                fee = estimate()
                feeCaution = null
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastError = e
                if (attempt < ESTIMATE_ATTEMPTS - 1) delay(ESTIMATE_RETRY_DELAY_MS)
            }
        }
        feeCaution = feeCaution(lastError ?: return)
    }

    private fun feeCaution(error: Throwable) = when (error) {
        is UnknownHostException -> CautionViewItem(
            title = Translator.getString(R.string.Hud_Text_NoInternet),
            text = Translator.getString(R.string.FeeSettings_Error_FeeEstimateFailed),
            type = CautionViewItem.Type.Error,
        )
        else -> CautionViewItem(
            title = Translator.getString(R.string.FeeSettings_Error_FeeEstimateFailed),
            text = error.message ?: error.javaClass.simpleName,
            type = CautionViewItem.Type.Error,
        )
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val transactionType = transactionType ?: throw IllegalStateException("Send data not set")

        when (transactionType) {
            is TransactionType.Boc -> {
                adapter.send(transactionType.boc)
            }

            is TransactionType.Regular -> {
                adapter.send(transactionType.amount, transactionType.address, transactionType.memo)
            }
        }

        return SendTransactionResult.Ton
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = fee?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOfNotNull(feeCaution),
        sendable = transactionType != null && fee != null,
        loading = transactionType == null || (fee == null && feeCaution == null),
        fields = listOf(),
    )

    companion object {
        private const val ESTIMATE_ATTEMPTS = 3
        private const val ESTIMATE_RETRY_DELAY_MS = 500L
    }

    sealed class TransactionType {
        data class Regular(val address: FriendlyAddress, val amount: BigDecimal, val memo: String?) : TransactionType()
        data class Boc(val boc: String) : TransactionType()
    }
}
