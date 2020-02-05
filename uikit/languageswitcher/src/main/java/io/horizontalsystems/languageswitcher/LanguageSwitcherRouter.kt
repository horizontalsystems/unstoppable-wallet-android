package io.horizontalsystems.languageswitcher

import io.horizontalsystems.core.SingleLiveEvent

class LanguageSwitcherRouter : LanguageSwitcherModule.IRouter {
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
