package io.horizontalsystems.currencyswitcher

import io.horizontalsystems.core.SingleLiveEvent

class CurrencySwitcherRouter : CurrencySwitcherModule.IRouter {
    val closeLiveEvent = SingleLiveEvent<Unit>()

    override fun close() {
        closeLiveEvent.call()
    }
}
