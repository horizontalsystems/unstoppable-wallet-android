package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

object ThorChainProvider : BaseThorChainProvider(
    baseUrl = "https://thornode.ninerealms.com/thorchain/",
    affiliate = "hrz",
    affiliateBps = 100,
) {
    override val id = "thorchain"
    override val title = "THORChain"
    override val icon = R.drawable.thorchain
    override val priority = 0
}
