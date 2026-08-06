package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.ThorchainAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class SendTransactionServiceThorchain(
    private val adapter: ThorchainAdapter,
    blockchainType: BlockchainType,
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Thorchain())

    // Fee is paid in the chain's native coin (RUNE on THORChain, CACAO on Maya).
    private val feeToken = App.coinManager.getToken(TokenQuery(blockchainType, TokenType.Native))
        ?: throw IllegalArgumentException("Native token not found for ${'$'}{blockchainType.uid}")

    private var sendData: SendTransactionData.Thorchain? = null
    private var cautions: List<CautionViewItem> = emptyList()

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Thorchain)

        sendData = data

        val amount = when (data) {
            is SendTransactionData.Thorchain.Deposit -> data.amount
            is SendTransactionData.Thorchain.Send -> data.amount
        }

        // the network fee is always paid in RUNE, on top of the swapped amount
        val insufficient = if (adapter.isNativeCoin) {
            amount + adapter.fee > adapter.availableBalance
        } else {
            amount > adapter.availableBalance || adapter.fee > adapter.runeAvailableBalance
        }

        cautions = if (insufficient) {
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
        val txHash = when (val data = sendData!!) {
            is SendTransactionData.Thorchain.Deposit -> {
                adapter.deposit(data.asset, data.amount, data.memo)
            }

            is SendTransactionData.Thorchain.Send -> {
                adapter.send(data.amount, data.address, data.memo)
            }
        }

        return SendTransactionResult.Thorchain(txHash = txHash)
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = getAmountData(CoinValue(feeToken, adapter.fee)),
        cautions = cautions,
        sendable = sendData != null && cautions.none { it.type == CautionViewItem.Type.Error },
        loading = sendData == null,
        fields = listOf(),
    )
}