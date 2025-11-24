package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.StellarKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

class SendTransactionServiceStellar(val stellarKit: StellarKit) : AbstractSendTransactionService(false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Stellar())

    private var fee: BigDecimal? = stellarKit.sendFee

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native)) ?: throw IllegalArgumentException()

    private var transactionEnvelope: String? = null

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Stellar)

        if (data is SendTransactionData.Stellar.WithTransactionEnvelope) {
            transactionEnvelope = data.transactionEnvelope
            fee = StellarKit.estimateFee(data.transactionEnvelope)
        }

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val transactionEnvelope = transactionEnvelope
        if (transactionEnvelope != null) {
            stellarKit.sendTransaction(transactionEnvelope)
        }

        return SendTransactionResult.Stellar
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = fee?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOf(),
        sendable = transactionEnvelope != null,
        loading = false,
        fields = listOf(),
        extraFees = extraFees
    )
}
