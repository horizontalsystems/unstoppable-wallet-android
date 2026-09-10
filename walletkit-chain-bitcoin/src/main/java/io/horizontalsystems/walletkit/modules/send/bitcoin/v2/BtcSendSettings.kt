package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.walletkit.core.chain.SendChainSettings

/** Coin control chosen on the send screen: the outputs to spend, or null for automatic selection. */
data class BtcSendSettings(
    val unspentOutputs: List<UnspentOutputInfo>?,
) : SendChainSettings
