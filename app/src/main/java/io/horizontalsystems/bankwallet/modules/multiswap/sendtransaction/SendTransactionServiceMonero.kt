package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.MoneroAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

class SendTransactionServiceMonero(
    private val adapter: MoneroAdapter
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Monero())

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Monero, TokenType.Native))
        ?: throw IllegalArgumentException()

    private var sendData: SendTransactionData.Monero? = null
    private var fee: BigDecimal? = null

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Monero)

        sendData = data
        fee = adapter.estimateFee(data.amount, data.address, data.memo)

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val data = sendData!!
        adapter.send(data.amount, data.address, data.memo)
        return SendTransactionResult.Monero
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = fee?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOf(),
        sendable = sendData != null && fee != null,
        loading = sendData == null || fee == null,
        fields = listOf(),
    )
}
