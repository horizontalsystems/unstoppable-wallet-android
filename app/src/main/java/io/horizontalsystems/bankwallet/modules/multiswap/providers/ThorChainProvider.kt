package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

object ThorChainProvider : BaseThorChainProvider() {
    override val id = "thorchain"
    override val title = "THORChain"
    override val url = "https://thorchain.org/swap"
    override val icon = R.drawable.thorchain
    override val priority = 0

    override val baseUrl = "https://thornode.ninerealms.com/thorchain"
    override val affiliate = "hrz"
    override val affiliateBps = 100
}
