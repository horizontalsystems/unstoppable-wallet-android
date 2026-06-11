package cash.p.terminal.modules.multiswap.providers

class SwapProvidersRegistry(
    changeNowProvider: ChangeNowProvider,
    quickexProvider: QuickexProvider,
    exolixProvider: ExolixProvider,
    stonFiProvider: StonFiProvider,
) {
    val providers: List<IMultiSwapProvider> = listOf(
        OneInchProvider,
        PancakeSwapProvider,
        PancakeSwapV3Provider,
        QuickSwapProvider,
        UniswapProvider,
        UniswapV3Provider,
        changeNowProvider,
        quickexProvider,
        exolixProvider,
        ThorChainProvider,
        MayaProvider,
        AllBridgeProvider,
        stonFiProvider,
    )

    fun findById(id: String): IMultiSwapProvider? =
        providers.firstOrNull { it.id == id }
}
