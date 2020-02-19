package io.horizontalsystems.currencyswitcher

import io.horizontalsystems.core.SingleLiveEvent

class BaseCurrencySettingsRouter : BaseCurrencySettingsModule.IRouter {
    val closeLiveEvent = SingleLiveEvent<Unit>()

    override fun close() {
        closeLiveEvent.call()
    }
}
