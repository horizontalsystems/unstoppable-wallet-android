package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.entities.Currency

class MainSettingsHelper {

    fun isBackedUp(nonBackedUpCount: Int): Boolean {
        return nonBackedUpCount == 0
    }

    fun displayName(baseCurrency: Currency): String {
        return baseCurrency.code
    }

}
