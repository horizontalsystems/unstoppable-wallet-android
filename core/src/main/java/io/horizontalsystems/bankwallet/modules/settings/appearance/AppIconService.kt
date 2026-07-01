package io.horizontalsystems.bankwallet.modules.settings.appearance

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppIconService(private val localStorage: ILocalStorage) {
    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.appIcon ?: AppIcon.Main, getAvailableIcons())
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setAppIcon(appIcon: AppIcon) {
        val targetIcon = if (appIcon.isDeprecated) {
            AppIcon.Main // Fallback to default if deprecated or unavailable
        } else {
            appIcon
        }

        localStorage.appIcon = targetIcon

        _optionsFlow.update {
            Select(targetIcon, getAvailableIcons())
        }

        applyIconChanges(targetIcon)
    }

    private fun applyIconChanges(selectedIcon: AppIcon) {
        val enabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val disabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        AppIcon.entries.forEach { icon ->
            try {
                val componentName = ComponentName(App.instance, icon.launcherName)
                val newState = if (selectedIcon == icon) enabled else disabled

                App.instance.packageManager.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                // Log the error but continue with other icons
                Log.e("LauncherIconManager", "Failed to set state for ${icon.launcherName}", e)
            }
        }
    }

    fun validateAndFixCurrentIcon() {
        val currentIcon = localStorage.appIcon

        when {
            currentIcon == null -> {
                setAppIcon(AppIcon.Main)
            }
            currentIcon.isDeprecated -> {
                // Current icon is deprecated or missing, fallback to default
                setAppIcon(AppIcon.Main)
            }
            else -> {
                //do nothing, current icon is valid
            }
        }
    }

    fun getAvailableIcons(): List<AppIcon> = AppIcon.getActiveIcons()
}
