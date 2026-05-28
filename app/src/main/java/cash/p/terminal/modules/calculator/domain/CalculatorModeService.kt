package cash.p.terminal.modules.calculator.domain

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.settings.appearance.AppIcon
import cash.p.terminal.modules.settings.appearance.AppIconService
import io.horizontalsystems.core.IPinComponent

class CalculatorModeService(
    private val localStorage: ILocalStorage,
    private val appIconService: AppIconService,
    private val pinComponent: IPinComponent,
) {

    fun enable(
        pinExistedBefore: Boolean,
        disablePushNotifications: Boolean = false,
    ) {
        if (localStorage.isCalculatorModeEnabled) return

        if (disablePushNotifications) {
            localStorage.pushNotificationsEnabled = false
        }
        localStorage.calculatorModeCreatedPin = !pinExistedBefore

        val currentIcon = localStorage.appIcon ?: AppIcon.Main
        if (currentIcon != AppIcon.Calculator) {
            localStorage.previousAppIconName = currentIcon.name
        }
        appIconService.setAppIcon(AppIcon.Calculator)
    }

    fun disable(keepPin: Boolean = false) {
        if (!localStorage.isCalculatorModeEnabled) return
        disableAndSwitchTo(previousAppIcon(), keepPin)
    }

    fun disableAfterPremiumLoss() {
        if (!localStorage.isCalculatorModeEnabled) return

        appIconService.setAppIcon(
            appIcon = previousAppIcon(),
            updateLauncherAliases = false,
        )
        localStorage.calculatorModeCreatedPin = false
        localStorage.calculatorModeLauncherAliasUpdatePending = true
    }

    fun disableAndSwitchTo(newIcon: AppIcon, keepPin: Boolean = false) {
        val shouldClearPin = !keepPin && localStorage.calculatorModeCreatedPin
        appIconService.setAppIcon(newIcon)
        if (shouldClearPin) {
            pinComponent.disablePin()
        }
        localStorage.calculatorModeCreatedPin = false
    }

    private fun previousAppIcon(): AppIcon =
        localStorage.previousAppIconName
            ?.let(AppIcon::fromString)
            ?: AppIcon.Main
}
