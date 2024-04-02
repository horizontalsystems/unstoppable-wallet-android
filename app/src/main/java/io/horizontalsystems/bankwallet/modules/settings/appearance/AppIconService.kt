package io.horizontalsystems.bankwallet.modules.settings.appearance

import android.content.ComponentName
import android.content.pm.PackageManager
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppIconService(private val localStorage: ILocalStorage) {
    private val appIcons by lazy { AppIcon.values().asList() }

    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.appIcon ?: AppIcon.Main, appIcons)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setAppIcon(appIcon: AppIcon) {
        localStorage.appIcon = appIcon

        _optionsFlow.update {
            Select(appIcon, appIcons)
        }

        val enabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val disabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        AppIcon.values().forEach { item ->
            App.instance.packageManager.setComponentEnabledSetting(
                ComponentName(App.instance, item.launcherName),
                if (appIcon == item) enabled else disabled,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
