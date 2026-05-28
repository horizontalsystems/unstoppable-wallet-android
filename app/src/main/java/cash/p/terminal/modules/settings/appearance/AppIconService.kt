package cash.p.terminal.modules.settings.appearance

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.ui_compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppIconService(private val localStorage: ILocalStorage) {
    private val appIcons by lazy { AppIcon.entries }

    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.appIcon ?: AppIcon.Main, appIcons)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    init {
        normalizeAppIconState()
    }

    /**
     * Ensures only one app icon alias is enabled and legacy aliases are disabled.
     * Handles both legacy icon migration and cases where multiple aliases are enabled.
     */
    private fun normalizeAppIconState() {
        if (localStorage.calculatorModeLauncherAliasUpdatePending) {
            // Applying the pending alias while the app is launched from Calculator can close
            // the current task on some launchers; MainActivity applies it after leaving foreground.
            return
        }

        // Disable any legacy aliases not in current AppIcon enum
        getLegacyAliases().forEach { disableComponentSafely(it) }

        // Determine the correct icon: use stored value, or Main if stored value is a legacy icon
        val appIcon = localStorage.appIcon ?: AppIcon.Main

        // Always apply to ensure only one alias is enabled
        setAppIcon(appIcon)
    }

    fun applyPendingLauncherAliasUpdate() {
        if (!localStorage.calculatorModeLauncherAliasUpdatePending) return

        localStorage.calculatorModeLauncherAliasUpdatePending = false
        setAppIcon(localStorage.appIcon ?: AppIcon.Main)
    }

    /**
     * Discovers all launcher aliases dynamically using PackageManager.
     * Returns component names of all activities matching MAIN/LAUNCHER intent.
     */
    private fun getAllLauncherAliases(): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return tryOrNull {
            App.instance.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }?.filter {
            it.activityInfo.packageName == App.instance.packageName
        }?.map {
            it.activityInfo.name
        } ?: emptyList()
    }

    /**
     * Returns aliases that exist in manifest but are not in current AppIcon enum.
     * These are legacy aliases that should be disabled.
     */
    private fun getLegacyAliases(): List<String> {
        val currentAliases = AppIcon.entries.map { it.launcherName }.toSet()
        return getAllLauncherAliases().filter { it !in currentAliases }
    }

    private fun disableComponentSafely(componentName: String) {
        tryOrNull {
            App.instance.packageManager.setComponentEnabledSetting(
                ComponentName(App.instance, componentName),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun setAppIcon(
        appIcon: AppIcon,
        updateLauncherAliases: Boolean = true,
    ) {
        // Keep the calculator-mode flag in lock-step with the persisted icon state so
        // settings UI, full-backup restore, and startup normalization cannot leave a
        // Calculator launcher paired with a normal PIN screen, or vice versa.
        // Foreground premium-loss handling may defer launcher alias updates because
        // disabling the alias that launched the current task can close it on some ROMs.
        val enableCalculatorMode = appIcon == AppIcon.Calculator
        if (enableCalculatorMode) {
            localStorage.isCalculatorModeEnabled = true
        }

        localStorage.appIcon = appIcon

        _optionsFlow.update {
            Select(appIcon, appIcons)
        }

        if (!updateLauncherAliases) {
            if (!enableCalculatorMode) {
                localStorage.isCalculatorModeEnabled = false
                localStorage.previousAppIconName = null
            }
            return
        }

        val enabled = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val disabled = PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        AppIcon.entries.forEach { item ->
            App.instance.packageManager.setComponentEnabledSetting(
                ComponentName(App.instance, item.launcherName),
                if (appIcon == item) enabled else disabled,
                PackageManager.DONT_KILL_APP
            )
        }

        if (!enableCalculatorMode) {
            localStorage.isCalculatorModeEnabled = false
            localStorage.previousAppIconName = null
        }
    }
}
