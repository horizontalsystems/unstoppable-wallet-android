package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.MoneroAdapter
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.providers.Translator
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
    private var cautions: List<CautionViewItem> = emptyList()

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Monero)

        sendData = data
        fee = adapter.estimateFee(data.amount, data.address, data.memo)

        val feeValue = fee ?: BigDecimal.ZERO
        val available = adapter.balanceData.available
        cautions = if (data.amount + feeValue > available) {
            listOf(
                CautionViewItem(
                    title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                    text = Translator.getString(R.string.Swap_ErrorInsufficientBalance),
                    type = CautionViewItem.Type.Error
                )
            )
        } else {
            emptyList()
        }

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
        cautions = cautions,
        sendable = sendData != null && fee != null && cautions.none { it.type == CautionViewItem.Type.Error },
        loading = sendData == null || fee == null,
        fields = listOf(),
    )
}
