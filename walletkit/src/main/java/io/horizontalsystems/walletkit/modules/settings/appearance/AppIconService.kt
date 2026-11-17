package io.horizontalsystems.walletkit.modules.settings.appearance

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.ui.compose.Select
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

        // The launcher draws a newly enabled entry for the mode of that
        // moment. Read the mode first: if it changes in between, the record
        // is the older one and the icon is drawn once more, never left stale.
        val drawing = runCatching { currentDrawing() }.getOrNull()

        applyIconChanges(targetIcon)

        drawing?.let { localStorage.launcherIconDrawnFor = it }
    }

    /**
     * Makes the launcher draw the icon again when the system's light or dark
     * mode is no longer the one it was drawn for.
     *
     * A launcher picks the icon's light or dark resources once, when the icon
     * is applied or the app is updated, and keeps that drawing when the mode
     * changes. It does draw again when a component of the app changes state,
     * so a component that is never enabled is moved between its two disabled
     * states. The app update time is part of the record because an update
     * makes the launcher draw too, in whatever mode the system is in then.
     */
    @Synchronized
    fun redrawLauncherIconIfOutdated() {
        val drawing = try {
            currentDrawing()
        } catch (e: Exception) {
            Log.e("LauncherIconManager", "Failed to read the launcher icon state", e)
            return
        }
        if (localStorage.launcherIconDrawnFor == drawing) return

        try {
            val packageManager = App.instance.packageManager
            val component = ComponentName(App.instance, "${App.instance.packageName}.$REDRAW_COMPONENT")
            val disabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            val newState = when (packageManager.getComponentEnabledSetting(component)) {
                disabled -> PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                else -> disabled
            }
            packageManager.setComponentEnabledSetting(component, newState, PackageManager.DONT_KILL_APP)

            localStorage.launcherIconDrawnFor = drawing
        } catch (e: Exception) {
            Log.e("LauncherIconManager", "Failed to redraw the launcher icon", e)
        }
    }

    // The system's mode, which a launcher follows, and not the app's own
    // theme: the application's resources carry the app's night mode override.
    private fun currentDrawing(): String {
        val uiMode = Resources.getSystem().configuration.uiMode
        val nightMode = uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val appUpdateTime = App.instance.packageManager
            .getPackageInfo(App.instance.packageName, 0)
            .lastUpdateTime

        return "${if (nightMode) "night" else "day"}:$appUpdateTime"
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

    companion object {
        // Declared in the manifest next to the launcher aliases
        private const val REDRAW_COMPONENT = "LauncherIconRedraw"
    }
}
