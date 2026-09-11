package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendStellarAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.send.SendErrorMinimumSendAmount
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.stellar.IStellarSender
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.stellar.StellarSenderRegular
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.stellar.StellarSenderTransactionEnvelope
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

    // A destination not yet on the ledger is created by the payment, which the network
    // accepts only from the base reserve upwards; below that the send would fail on
    // broadcast, so it is refused here.
    private var minimumAmountCaution: CautionViewItem? = null
    private var checkingMinimum = false

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

        if (data is SendTransactionData.Stellar.Regular) {
            checkingMinimum = true
            minimumAmountCaution = null
            emitState()

            minimumAmountCaution = try {
                val minimum = App.adapterManager.getAdapterForToken<ISendStellarAdapter>(token)
                    ?.getMinimumSendAmount(data.address)
                if (minimum != null && data.amount < minimum) {
                    SendErrorMinimumSendAmount(minimum, token.coin.code).toCautionViewItem()
                } else {
                    null
                }
            } catch (e: Throwable) {
                // Without knowing whether the destination exists the payment may fail on
                // broadcast; surface the lookup failure rather than guess.
                CautionViewItem.fromThrowable(e)
            }
            checkingMinimum = false
        }

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val txHash = stellarSendHandler!!.sendTransaction()

        return SendTransactionResult.Stellar(txHash)
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = stellarSendHandler?.getFee()?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOfNotNull(minimumAmountCaution),
        sendable = stellarSendHandler != null && !checkingMinimum && minimumAmountCaution == null,
        loading = stellarSendHandler == null || checkingMinimum,
        fields = listOf(),
    )
}
