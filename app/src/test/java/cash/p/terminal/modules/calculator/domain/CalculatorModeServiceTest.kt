package cash.p.terminal.modules.calculator.domain

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.settings.appearance.AppIcon
import cash.p.terminal.modules.settings.appearance.AppIconService
import io.horizontalsystems.core.IPinComponent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CalculatorModeServiceTest {

    private lateinit var localStorage: ILocalStorage
    private lateinit var appIconService: AppIconService
    private lateinit var pinComponent: IPinComponent
    private lateinit var service: CalculatorModeService

    @Before
    fun setUp() {
        localStorage = mockk(relaxed = true)
        appIconService = mockk(relaxed = true)
        pinComponent = mockk(relaxed = true)
        service = CalculatorModeService(
            localStorage = localStorage,
            appIconService = appIconService,
            pinComponent = pinComponent,
        )
    }

    @Test
    fun enable_pinDidNotExistBefore_marksCalculatorModeCreatedPin() {
        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.appIcon } returns AppIcon.Main

        service.enable(pinExistedBefore = false)

        verify { localStorage.calculatorModeCreatedPin = true }
    }

    @Test
    fun enable_pinExistedBefore_marksCalculatorModeDidNotCreatePin() {
        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.appIcon } returns AppIcon.Main

        service.enable(pinExistedBefore = true)

        verify { localStorage.calculatorModeCreatedPin = false }
    }

    @Test
    fun enable_alreadyEnabled_doesNotTouchCreatedPinFlag() {
        every { localStorage.isCalculatorModeEnabled } returns true

        service.enable(pinExistedBefore = false)

        verify(exactly = 0) { localStorage.calculatorModeCreatedPin = any() }
        verify(exactly = 0) { appIconService.setAppIcon(any()) }
    }

    @Test
    fun disable_calculatorCreatedPin_disablesPinAndClearsFlag() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns true
        every { localStorage.previousAppIconName } returns AppIcon.Main.name

        service.disable()

        verify { appIconService.setAppIcon(AppIcon.Main) }
        verify { pinComponent.disablePin() }
        verify { localStorage.calculatorModeCreatedPin = false }
    }

    @Test
    fun disable_keepPinTrue_keepsPinAndClearsFlag() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns true
        every { localStorage.previousAppIconName } returns AppIcon.Main.name

        service.disable(keepPin = true)

        verify { appIconService.setAppIcon(AppIcon.Main) }
        verify(exactly = 0) { pinComponent.disablePin() }
        verify { localStorage.calculatorModeCreatedPin = false }
    }

    @Test
    fun disableAfterPremiumLoss_calculatorCreatedPin_keepsPinAndDefersLauncherAliasUpdate() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns true
        every { localStorage.previousAppIconName } returns AppIcon.Pirate.name

        service.disableAfterPremiumLoss()

        verify { appIconService.setAppIcon(AppIcon.Pirate, updateLauncherAliases = false) }
        verify(exactly = 0) { pinComponent.disablePin() }
        verify { localStorage.calculatorModeCreatedPin = false }
        verify { localStorage.calculatorModeLauncherAliasUpdatePending = true }
    }

    @Test
    fun disableAfterPremiumLoss_alreadyDisabled_doesNothing() {
        every { localStorage.isCalculatorModeEnabled } returns false

        service.disableAfterPremiumLoss()

        verify(exactly = 0) { appIconService.setAppIcon(any(), updateLauncherAliases = any()) }
        verify(exactly = 0) { pinComponent.disablePin() }
        verify(exactly = 0) { localStorage.calculatorModeLauncherAliasUpdatePending = any() }
    }

    @Test
    fun disable_calculatorDidNotCreatePin_keepsPin() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns false
        every { localStorage.previousAppIconName } returns AppIcon.Main.name

        service.disable()

        verify { appIconService.setAppIcon(AppIcon.Main) }
        verify(exactly = 0) { pinComponent.disablePin() }
    }

    @Test
    fun disable_alreadyDisabled_doesNothing() {
        every { localStorage.isCalculatorModeEnabled } returns false

        service.disable()

        verify(exactly = 0) { appIconService.setAppIcon(any()) }
        verify(exactly = 0) { pinComponent.disablePin() }
        verify(exactly = 0) { localStorage.calculatorModeCreatedPin = any() }
    }

    @Test
    fun disableAndSwitchTo_calculatorCreatedPin_disablesPinAndClearsFlag() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns true

        service.disableAndSwitchTo(AppIcon.Pirate)

        verify { appIconService.setAppIcon(AppIcon.Pirate) }
        verify { pinComponent.disablePin() }
        verify { localStorage.calculatorModeCreatedPin = false }
    }

    @Test
    fun disableAndSwitchTo_keepPinTrue_keepsPinAndClearsFlag() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns true

        service.disableAndSwitchTo(AppIcon.Pirate, keepPin = true)

        verify { appIconService.setAppIcon(AppIcon.Pirate) }
        verify(exactly = 0) { pinComponent.disablePin() }
        verify { localStorage.calculatorModeCreatedPin = false }
    }

    @Test
    fun disableAndSwitchTo_calculatorDidNotCreatePin_keepsPin() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorModeCreatedPin } returns false

        service.disableAndSwitchTo(AppIcon.Pirate)

        verify { appIconService.setAppIcon(AppIcon.Pirate) }
        verify(exactly = 0) { pinComponent.disablePin() }
    }
}
