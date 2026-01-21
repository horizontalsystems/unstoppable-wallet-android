package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.model.Proposal
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

class SendTransactionServiceZcash(
    private val adapter: ZcashAdapter
) : AbstractSendTransactionService(false, false) {
    override val sendTransactionSettingsFlow = MutableStateFlow(SendTransactionSettings.Zcash())

    private val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Zcash, TokenType.Native))
        ?: throw IllegalArgumentException()

    private var proposal: Proposal? = null
    private var fee: BigDecimal? = null

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Zcash)

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

        proposal = adapter.createProposal(outputs)
        fee = proposal?.totalFeeRequired()?.convertZatoshiToZec()

        emitState()
    }

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        adapter.sendProposal(proposal!!)
        return SendTransactionResult.Zcash
    }

    override fun createState() = SendTransactionServiceState(
        uuid = uuid,
        networkFee = fee?.let {
            getAmountData(CoinValue(feeToken, it))
        },
        cautions = listOf(),
        sendable = proposal != null,
        loading = proposal == null,
        fields = listOf(),
    )

}
