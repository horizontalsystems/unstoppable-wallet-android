package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.core.App

object MultiSwapProviderRegistry {
    /**
     * The one provider every swap, private send and CrossPay goes through: one `/v3/rate` answers
     * with every venue's route, so the token catalog, the provider directory and the session are
     * shared by every screen.
     */
    val uswap: USwapProvider by lazy {
        USwapProvider(App.swapProviderInfoManager, App.uSwapAssetCatalog)
    }

    /** What the swap screen quotes. The Private (split) tab quotes the same instance through the split endpoints. */
    val allProviders: List<IMultiSwapProvider>
        get() = listOf(uswap)

    val splitProviders: List<ISplitSwapProvider>
        get() = listOf(uswap)
}
