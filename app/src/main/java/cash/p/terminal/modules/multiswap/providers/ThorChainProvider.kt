package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.ui_compose.R

object ThorChainProvider : BaseThorChainProvider(
    baseUrl = "https://thornode.ninerealms.com/thorchain/",
    affiliate = "hrz",
    affiliateBps = 100,
) {
    override val id = "thorchain"
    override val title = "THORChain"
    override val icon = R.drawable.thorchain
}
