package io.horizontalsystems.walletkit.core.adapters

/** Adapters that expose diagnostic status for the App Status screen. */
interface IStatusInfoAdapter {
    val statusInfo: Map<String, Any>
}
