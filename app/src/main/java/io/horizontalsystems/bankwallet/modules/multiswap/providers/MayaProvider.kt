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
    affiliateBps = 100,
) {
    override val id = "mayachain"
    override val title = "Maya Protocol"
    override val icon = R.drawable.maya
    override val priority = 0

    override fun getRefundAddress(tokenIn: Token): String? {
        return if (tokenIn.blockchainType == BlockchainType.Zcash) {
            App.adapterManager.getAdapterForToken<ZcashAdapter>(tokenIn)?.receiveAddressTransparent
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

            return SendTransactionData.Zcash.ShieldedMemo(
                address = quoteSwap.inbound_address,
                amount = amountIn,
                memo = quoteSwap.memo,
                memoShieldedAddress = shieldedMemoConfig.unified_address
            )
        }

        return super.getSendTransactionData(tokenIn, amountIn, quoteSwap, tokenOut)
    }
}
