package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.litecoinmwebkit.MwebSendError
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ISendMwebAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.factories.FeeRateProviderFactory
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * MWEB-to-MWEB sends and peg-outs. The fee rate is Litecoin's recommended rate: a peg-out
 * pays for its Litecoin-side output at that rate, and the kit scales the MWEB weight fee
 * from it as well.
 */
class SendTransactionServiceMweb(
    private val adapter: ISendMwebAdapter,
    private val token: Token,
) : AbstractSendTransactionService(false, false) {

    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Mweb())

    private var feeRate: Int? = null
    private var sendData: SendTransactionData.Mweb? = null
    private var fee: BigDecimal? = null
    private var maxSendable: BigDecimal? = null
    private var error: Throwable? = null

    override fun start(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            feeRate = try {
                FeeRateProviderFactory.provider(BlockchainType.Litecoin)?.getFeeRates()?.recommended
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            } ?: FALLBACK_FEE_RATE
            refresh()
            emitState()
        }
    }

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Mweb)
        sendData = data
        refresh()
        emitState()
    }

    private suspend fun refresh() {
        val data = sendData ?: return
        val rate = feeRate ?: return
        error = null
        maxSendable = try {
            adapter.maximumSendable(data.address, rate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        }
        fee = try {
            adapter.estimateFee(data.amount, data.address, rate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            error = e
            null
        }
    }

    // Computed with the fee in [refresh], so it is read after the state has settled.
    override fun maxSendableAmount(): BigDecimal? = maxSendable

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val data = sendData!!
        val uid = adapter.send(data.amount, data.address, feeRate!!)
        return SendTransactionResult.Mweb(uid)
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = fee?.let { getAmountData(CoinValue(token, it)) },
        cautions = listOfNotNull(error?.let { caution(it) }),
        sendable = sendData != null && fee != null && error == null,
        loading = sendData == null || feeRate == null || (fee == null && error == null),
        fields = listOf(),
    )

    private fun caution(error: Throwable): CautionViewItem = when (error) {
        is MwebSendError.InsufficientBalance -> CautionViewItem(
            title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
            text = Translator.getString(R.string.Swap_ErrorInsufficientBalance),
            type = CautionViewItem.Type.Error,
        )
        is MwebSendError.NotSynced -> CautionViewItem(
            title = Translator.getString(R.string.Balance_Syncing),
            text = error.message ?: "",
            type = CautionViewItem.Type.Error,
        )
        else -> CautionViewItem.fromThrowable(error)
    }

    companion object {
        // Litecoin's floor rate, used only when the fee provider is unreachable.
        private const val FALLBACK_FEE_RATE = 1
    }
}
