package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

object MayaProvider : BaseThorChainProvider(
    baseUrl = "https://mayanode.mayachain.info/mayachain/",
    affiliate = "hrz_android",
    affiliateBps = App.appConfigProvider.swapFeeBps,
) {
    override val id = MAYA_PROVIDER_ID
    override val title = "Maya Protocol"
    override val riskLevel = RiskLevel.EXCELLENT

    // Maya settles pools in CACAO, not RUNE — without this, swaps to/from CACAO show no route.
    override val settlementBlockchainType = BlockchainType.Mayachain
    override val settlementAsset = "MAYA.CACAO"

    override suspend fun resolveDestinationAddress(tokenOut: Token): String {
        // Maya delivers ZEC directly to unified/sapling/orchard receivers via its transparent
        // vault, so for ZEC out we resolve the wallet's unified address. Every other tokenOut
        // goes through the generic destination resolver.
        if (tokenOut.blockchainType == BlockchainType.Zcash) {
            return ChainRegistry[BlockchainType.Zcash]?.swapUnifiedReceiveAddress(tokenOut)
                ?: throw IllegalStateException("Zcash support is not available")
        }
        return super.resolveDestinationAddress(tokenOut)
    }

    override fun getRefundAddress(tokenIn: Token): String? {
        return if (tokenIn.blockchainType == BlockchainType.Zcash) {
            App.adapterManager.getAdapterForToken<IReceiveAdapter>(tokenIn)?.receiveAddressTransparent
        } else {
            null
        }
    }

    override fun getFromAddress(tokenIn: Token): String? {
        return if (tokenIn.blockchainType == BlockchainType.Zcash) {
            App.adapterManager.getAdapterForToken<IReceiveAdapter>(tokenIn)?.receiveAddress
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
                address = checkNotNull(quoteSwap.inbound_address),
                amount = amountIn,
                memo = quoteSwap.memo,
                memoShieldedAddress = shieldedMemoConfig.unified_address
            )
        }

        return super.getSendTransactionData(tokenIn, amountIn, quoteSwap, tokenOut)
    }
}
