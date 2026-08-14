package io.horizontalsystems.walletkit.modules.multiswap.providers


object ThorChainProvider : BaseThorChainProvider(
    baseUrl = "https://gateway.liquify.com/chain/thorchain_api/thorchain/",
    affiliate = "hrz",
    affiliateBps = 100,
) {
    override val streamingInterval: Long = 0
    override val id = THORCHAIN_PROVIDER_ID
    override val title = "THORChain"
    override val riskLevel = RiskLevel.EXCELLENT
    override val securedAssetsSupported = true
}
