package cash.p.terminal.modules.multiswap.sendtransaction

import cash.p.terminal.entities.CoinValue
import cash.p.terminal.strings.R
import cash.p.terminal.wallet.Token
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigDecimal

sealed class SendTransactionData {
    data class Common(
        val amount: BigDecimal,
        val memo: String?,
        val address: String,
        val dustThreshold: Int?,
        val changeToFirstInput: Boolean,
        val utxoFilters: UtxoFilters,
        val recommendedGasRate: Int?,
        val feesMap: Map<FeeType, CoinValue>?,
        val token: Token
    ) :
        SendTransactionData()

    data class Evm(
        val transactionData: TransactionData,
        val gasLimit: Long?,
        val feesMap: Map<FeeType, CoinValue> = mapOf()
    ) : SendTransactionData()
}

enum class FeeType(val stringResId: Int) {
    Outbound(R.string.Fee_OutboundFee),
    Liquidity(R.string.Fee_LiquidityFee);
}
