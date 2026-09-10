package io.horizontalsystems.walletkit.modules.send.monero.v2

import io.horizontalsystems.walletkit.core.MoneroUnspentOutput
import io.horizontalsystems.walletkit.core.chain.SendChainSettings

/** Coin control chosen on the send screen: the outputs to spend, or null for the wallet's own selection. */
data class MoneroSendSettings(
    val unspentOutputs: List<MoneroUnspentOutput>? = null,
) : SendChainSettings
