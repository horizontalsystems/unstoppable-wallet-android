package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

object ThorChainProvider : BaseThorChainProvider(
    baseUrl = "https://gateway.liquify.com/chain/thorchain_api/thorchain/",
    affiliate = "hrz",
    affiliateBps = 100,
) {
    override val id = "thorchain"
    override val title = "THORChain"
    override val icon = R.drawable.swap_provider_thorchain
    override val riskLevel = RiskLevel.AUTO
}
