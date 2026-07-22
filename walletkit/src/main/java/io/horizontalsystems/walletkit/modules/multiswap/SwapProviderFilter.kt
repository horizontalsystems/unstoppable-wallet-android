package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider

/**
 * Registration seam for restricting the swap provider set per account. Some
 * account types can only execute a subset of providers (e.g. smart accounts
 * whose swaps are batched into their own transaction pipeline); quoting with
 * an inexecutable provider would advertise a quote the confirm step must
 * reject. A null provider (or a null result) keeps the full registry.
 */
object SwapProviderFilter {

    interface Provider {
        fun providers(all: List<IMultiSwapProvider>): List<IMultiSwapProvider>?
    }

    @Volatile
    var provider: Provider? = null
}
