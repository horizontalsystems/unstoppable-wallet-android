package io.horizontalsystems.bankwallet.modules.settings.language

import io.horizontalsystems.bankwallet.SingleLiveEvent

class LanguageSettingsRouter: LanguageSettingsModule.ILanguageSettingsRouter {
    val reloadAppLiveEvent = SingleLiveEvent<Unit>()

    override fun reloadAppInterface() {
        reloadAppLiveEvent.call()
    }
}
