package io.horizontalsystems.bankwallet.modules.settings.language

import io.horizontalsystems.core.SingleLiveEvent

class LanguageSettingsRouter: LanguageSettingsModule.ILanguageSettingsRouter {
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
