package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.chain.SendChainSettings

/**
 * Holds the chain-specific choices made on [SendV2Page] so its settings pages and
 * [SendV2ConfirmPage] can share them. Scoped to the send page; it has no constructor
 * arguments so it is recreated empty (chain defaults) after process death.
 */
class SendChainSettingsViewModel : ViewModel() {
    var settings by mutableStateOf<SendChainSettings?>(null)
}
