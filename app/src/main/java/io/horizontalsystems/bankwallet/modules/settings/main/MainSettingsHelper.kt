package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.core.entities.Currency

class MainSettingsHelper {

    fun displayName(baseCurrency: Currency): String {
        return baseCurrency.code
    }

}
