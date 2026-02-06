package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

object MayaProvider : BaseThorChainProvider(
    baseUrl = "https://mayanode.mayachain.info/mayachain/",
    affiliate = "hrz_android",
    affiliateBps = 50,
) {
    override val id = "mayachain"
    override val title = "Maya Protocol"
    override val icon = R.drawable.swap_provider_maya

    // When true, refundAddress is not passed to the quoteSwap API request
    // but is manually added to the memo returned by the API.
    // This avoids the "generated memo too long for source chain" error
    // for UTXO chains (e.g. Zcash) where the memo with refundAddress exceeds
    // the 80-char OP_RETURN limit.
    private const val MANUALLY_ADD_REFUND_ADDRESS = true

    override fun getRefundAddress(tokenIn: Token): String? {
        return if (tokenIn.blockchainType == BlockchainType.Zcash) {
            if (MANUALLY_ADD_REFUND_ADDRESS) {
                null
            } else {
                App.adapterManager.getAdapterForToken<ZcashAdapter>(tokenIn)?.receiveAddressTransparent
            }
        } else {
            null
        }
    }

    override suspend fun getSendTransactionData(
        tokenIn: Token,
        amountIn: BigDecimal,
        quoteSwap: ThornodeAPI.Response.QuoteSwap,
        tokenOut: Token,
    ): SendTransactionData {
        if (tokenIn.blockchainType == BlockchainType.Zcash) {
            val inboundAddresses = thornodeAPI.inboundAddresses()
            val shieldedMemoConfig = inboundAddresses
                .find { it.chain == "ZEC" }
                ?.shielded_memo_config

            if (shieldedMemoConfig == null || !shieldedMemoConfig.enabled) {
                throw IllegalStateException("Zcash shielded memo is not available or disabled")
            }

            var memo = quoteSwap.memo

            if (MANUALLY_ADD_REFUND_ADDRESS) {
                val refundAddress = App.adapterManager.getAdapterForToken<ZcashAdapter>(tokenIn)?.receiveAddressTransparent
                if (refundAddress != null) {
                    memo = addRefundAddressToMemo(memo, refundAddress)
                }
            }

            return SendTransactionData.Zcash.ShieldedMemo(
                address = quoteSwap.inbound_address,
                amount = amountIn,
                memo = memo,
                memoShieldedAddress = shieldedMemoConfig.unified_address
            )
        }

        return super.getSendTransactionData(tokenIn, amountIn, quoteSwap, tokenOut)
    }

    // Memo format: =:ASSET:DESTADDR:LIM/INTERVAL/QUANTITY:AFFILIATE:FEE
    // Adds refund address after DESTADDR: =:ASSET:DESTADDR/REFUNDADDR:LIM/INTERVAL/QUANTITY:AFFILIATE:FEE
    private fun addRefundAddressToMemo(memo: String, refundAddress: String): String {
        val parts = memo.split(":")
        if (parts.size < 3) return memo
        val mutableParts = parts.toMutableList()
        mutableParts[2] = "${mutableParts[2]}/$refundAddress"
        return mutableParts.joinToString(":")
    }
}
