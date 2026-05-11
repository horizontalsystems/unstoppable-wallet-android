package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.ZanoAdapter
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.providers.Translator
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
    private var cautions: List<CautionViewItem> = emptyList()

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Zano)

        sendData = data
        fee = adapter.estimateFee(data.amount, data.address, data.memo)

        cautions = buildCautions(data, fee)

        emitState()
    }

    private fun buildCautions(data: SendTransactionData.Zano, fee: BigDecimal?): List<CautionViewItem> {
        val feeValue = fee ?: return emptyList()
        val result = mutableListOf<CautionViewItem>()

        if (adapter.isNativeAsset) {
            val available = adapter.balanceData.available
            if (data.amount + feeValue > available) {
                result.add(
                    CautionViewItem(
                        title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                        text = Translator.getString(R.string.Swap_ErrorInsufficientBalance),
                        type = CautionViewItem.Type.Error
                    )
                )
            }
        } else {
            val assetAvailable = adapter.balanceData.available
            if (data.amount > assetAvailable) {
                result.add(
                    CautionViewItem(
                        title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                        text = Translator.getString(R.string.Swap_ErrorInsufficientBalance),
                        type = CautionViewItem.Type.Error
                    )
                )
            }
            val nativeAvailable = adapter.nativeAvailableBalance
            if (feeValue > nativeAvailable) {
                result.add(
                    CautionViewItem(
                        title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                        text = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, feeToken.coin.code),
                        type = CautionViewItem.Type.Error
                    )
                )
            }
        }

        return result
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
        cautions = cautions,
        sendable = sendData != null && fee != null && cautions.none { it.type == CautionViewItem.Type.Error },
        loading = sendData == null || fee == null,
        fields = listOf(),
    )
}
