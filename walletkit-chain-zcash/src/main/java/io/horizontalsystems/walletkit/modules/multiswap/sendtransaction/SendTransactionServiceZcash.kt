package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.Proposal
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.coinCodeWithNetwork
import io.horizontalsystems.walletkit.core.InsufficientBalance
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

class SendTransactionServiceZcash(
    private val adapter: ZcashAdapter,
    // Runs once the transaction is on its way: a swap, CrossPay or Private Send deposit ends
    // the operation whose refund address the chain plugin kept, so the next one names a new one.
    private val onSent: () -> Unit = {},
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Zcash())

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Zcash, TokenType.Native))
        ?: throw IllegalArgumentException()

    private var proposal: Proposal? = null
    private var error: Throwable? = null
    private var fee: BigDecimal? = null
    private var maxSendable: BigDecimal? = null

    // Only until a proposal succeeds: the fee of the full amount includes a change output that
    // the lowered amount does not need, so its own fee then gives the exact maximum.
    override fun maxSendableAmount(): BigDecimal? = maxSendable.takeIf { proposal == null }

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Zcash)

        val amount = when (data) {
            is SendTransactionData.Zcash.Regular -> data.amount
            is SendTransactionData.Zcash.ShieldedMemo -> data.amount
        }
        val outputs = when (data) {
            is SendTransactionData.Zcash.Regular -> {
                listOf(
                    ZcashAdapter.TransferOutput(
                        amount = data.amount,
                        address = data.address,
                        memo = data.memo
                    )
                )
            }

            is SendTransactionData.Zcash.ShieldedMemo -> {
                listOf(
                    ZcashAdapter.TransferOutput(
                        amount = data.amount,
                        address = data.address,
                        memo = ""
                    ),
                    ZcashAdapter.TransferOutput(
                        amount = BigDecimal.ZERO,
                        address = data.memoShieldedAddress,
                        memo = data.memo
                    )
                )
            }
        }

        error = null
        proposal = null
        fee = null
        try {
            proposal = adapter.createProposal(outputs)
            fee = proposal?.totalFeeRequired()?.convertZatoshiToZec()
        } catch (e: Throwable) {
            val message = e.message
            maxSendable = maxSendableZatoshi(message, amount.convertZecToZatoshi().value)
                ?.let { Zatoshi(it).convertZatoshiToZec() }
            error = if (message != null && message.contains("Insufficient balance", ignoreCase = true)) {
                InsufficientBalance(feeToken.coinCodeWithNetwork)
            } else {
                e
            }
        }

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val transactionHash = adapter.sendProposal(proposal!!)
        onSent()
        return SendTransactionResult.Zcash(transactionHash)
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = fee?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOfNotNull(error?.let { CautionViewItem.fromThrowable(it) }),
        sendable = proposal != null && error == null,
        loading = proposal == null && error == null,
        fields = listOf(),
    )


    companion object {
        private val insufficientFunds = Regex("""Insufficient balance \(have (\d+), need (\d+) including fee\)""")

        /**
         * What a transfer of [amount] zatoshi the wallet cannot fund could be lowered to, or null.
         * The SDK prices only a transfer it can fund, so sending the whole balance leaves no fee
         * to take out of it; its error names what the wallet has and what the transfer needs,
         * fee included.
         */
        fun maxSendableZatoshi(message: String?, amount: Long): Long? {
            val match = insufficientFunds.find(message ?: return null) ?: return null
            val have = match.groupValues[1].toLongOrNull() ?: return null
            val need = match.groupValues[2].toLongOrNull() ?: return null
            val fee = need - amount
            if (fee <= 0) return null
            return (have - fee).takeIf { it > 0 }
        }
    }
}
