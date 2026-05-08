package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.ZanoAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

class SendTransactionServiceZano(
    private val adapter: ZanoAdapter
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Zano())

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Zano, TokenType.Native))
        ?: throw IllegalArgumentException()

    private var sendData: SendTransactionData.Zano? = null
    private var fee: BigDecimal? = null

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Zano)

        sendData = data
        fee = adapter.estimateFee(data.amount, data.address, data.memo)

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val data = sendData!!
        adapter.send(data.amount, data.address, data.memo)
        return SendTransactionResult.Zano
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
