package cash.p.terminal.modules.settings.appearance

import android.content.pm.PackageManager
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import io.horizontalsystems.core.CoreApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AppIconServiceTest {

    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val packageManager = mockk<PackageManager>(relaxed = true)
    private val app = mockk<CoreApp>(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(App)
        every { App.instance } returns app
        every { app.packageName } returns "cash.p.terminal.dev"
        every { app.packageManager } returns packageManager
        every { localStorage.appIcon } returns AppIcon.Main
    }

    @After
    fun tearDown() {
        unmockkObject(App)
    }

    @Test
    fun init_pendingLauncherAliasUpdate_doesNotNormalizeAliases() {
        every { localStorage.calculatorModeLauncherAliasUpdatePending } returns true

        AppIconService(localStorage)

        verify(exactly = 0) {
            packageManager.setComponentEnabledSetting(any(), any(), any())
        }
    }

    @Test
    fun applyPendingLauncherAliasUpdate_pending_appliesAliasesAndClearsPending() {
        every { localStorage.calculatorModeLauncherAliasUpdatePending } returns true
        every { localStorage.appIcon } returns AppIcon.Pirate
        val service = AppIconService(localStorage)

        service.applyPendingLauncherAliasUpdate()

        verify { localStorage.calculatorModeLauncherAliasUpdatePending = false }
        verify(exactly = 1) {
            packageManager.setComponentEnabledSetting(
                any(),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        verify(exactly = AppIcon.entries.size - 1) {
            packageManager.setComponentEnabledSetting(
                any(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
