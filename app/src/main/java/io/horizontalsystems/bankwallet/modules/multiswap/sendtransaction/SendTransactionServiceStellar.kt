package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.stellar.IStellarSender
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.stellar.StellarSenderRegular
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.stellar.StellarSenderTransactionEnvelope
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.StellarKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class SendTransactionServiceStellar(
    private val stellarKit: StellarKit,
    private val token: Token
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Stellar())

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native)) ?: throw IllegalArgumentException()

    private var stellarSendHandler: IStellarSender? = null

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Stellar)

        stellarSendHandler = when (data) {
            is SendTransactionData.Stellar.Regular -> {
                StellarSenderRegular(
                    data.address,
                    data.amount,
                    data.memo,
                    stellarKit,
                    token
                )
            }

            is SendTransactionData.Stellar.WithTransactionEnvelope -> {
                StellarSenderTransactionEnvelope(data.transactionEnvelope, stellarKit)
            }
        }

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        stellarSendHandler!!.sendTransaction()

        return SendTransactionResult.Stellar
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = stellarSendHandler?.getFee()?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOf(),
        sendable = stellarSendHandler != null,
        loading = stellarSendHandler == null,
        fields = listOf(),
        extraFees = extraFees
    )
}
