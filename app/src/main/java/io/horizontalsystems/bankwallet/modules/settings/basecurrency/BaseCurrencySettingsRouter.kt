package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import io.horizontalsystems.core.SingleLiveEvent

class BaseCurrencySettingsRouter : BaseCurrencySettingsModule.IRouter {
    val closeLiveEvent = SingleLiveEvent<Unit>()

    override fun close() {
        closeLiveEvent.call()
    }
}
