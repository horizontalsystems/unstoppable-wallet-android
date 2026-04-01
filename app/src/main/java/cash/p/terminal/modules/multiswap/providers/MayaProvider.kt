package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.R

object MayaProvider : BaseThorChainProvider(
    baseUrl = "https://mayanode.mayachain.info/mayachain/",
    affiliate = null,
    affiliateBps = null,
) {
    override val id = "mayachain"
    override val title = "Maya Protocol"
    override val icon = R.drawable.maya
}