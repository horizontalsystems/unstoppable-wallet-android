package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import com.tonapps.wallet.data.core.entity.SendRequestEntity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.FriendlyAddress
import io.horizontalsystems.tonkit.core.TonKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

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

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Ton)

        when (data) {
            is SendTransactionData.Ton.Regular -> {
                val address = FriendlyAddress.parse(data.address)
                fee = adapter.estimateFee(data.amount, address, data.memo)
                transactionType = TransactionType.Regular(address, data.amount, data.memo)
            }

            is SendTransactionData.Ton.SendRequest -> {
                val request = SendRequestEntity(
                    data = data.requestJson,
                    tonConnectRequestId = "",
                    dAppId = ""
                )

                val boc = adapter.sign(request)
                fee = adapter.estimateFee(boc)
                transactionType = TransactionType.Boc(boc)
            }
        }

        emitState()
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
        cautions = listOf(),
        sendable = transactionType != null,
        loading = transactionType == null,
        fields = listOf(),
    )

    sealed class TransactionType {
        data class Regular(val address: FriendlyAddress, val amount: BigDecimal, val memo: String?) : TransactionType()
        data class Boc(val boc: String) : TransactionType()
    }
}
