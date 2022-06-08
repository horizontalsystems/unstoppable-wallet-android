package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeService

object AppearanceModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val launchScreenService = LaunchScreenService(App.localStorage)
            val themeService = ThemeService(App.localStorage)
            val balanceViewTypeService = BalanceViewTypeService(App.localStorage)
            return AppearanceViewModel(
                launchScreenService,
                themeService,
                App.baseCoinManager,
                balanceViewTypeService
            ) as T
        }
    }

}
