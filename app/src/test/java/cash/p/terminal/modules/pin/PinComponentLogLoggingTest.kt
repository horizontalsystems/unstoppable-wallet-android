package cash.p.terminal.modules.pin

import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.domain.usecase.DeleteAllContactsUseCase
import cash.p.terminal.domain.usecase.ResetUseCase
import cash.p.terminal.modules.pin.core.Pin
import cash.p.terminal.modules.pin.core.PinDao
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.modules.pin.core.PinLevels
import cash.p.terminal.modules.pin.core.PinManager
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IPinSettingsStorage
import io.horizontalsystems.core.CoreApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinComponentLogLoggingTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val pinDao = LogLoggingTestPinDao()
    private val pinDbStorage = PinDbStorage(pinDao)
    private val pinManager = PinManager(pinDbStorage)
    private val userManager = mockk<DefaultUserManager>(relaxed = true)
    private val pinSettingsStorage = mockk<IPinSettingsStorage>(relaxed = true)
    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)
    private val resetUseCase = mockk<ResetUseCase>(relaxed = true)
    private val deleteAllContactsUseCase = mockk<DeleteAllContactsUseCase>(relaxed = true)
    private lateinit var pinComponent: PinComponent
    private var currentUserLevel = 0

    @Before
    fun setup() {
        pinDao.reset()
        currentUserLevel = 0

        mockkObject(App)
        every { App.localStorage } returns mockk<ILocalStorage>(relaxed = true)
        every { App.instance } returns mockk<CoreApp>(relaxed = true)

        every { userManager.getUserLevel() } answers { currentUserLevel }
        every { userManager.setUserLevel(any()) } answers {
            currentUserLevel = firstArg()
            Unit
        }
        every { userManager.currentUserLevelFlow } returns MutableStateFlow(currentUserLevel)

        pinComponent = PinComponent(
            pinSettingsStorage = pinSettingsStorage,
            userManager = userManager,
            pinDbStorage = pinDbStorage,
            backgroundManager = backgroundManager,
            resetUseCase = resetUseCase,
            deleteAllContactsUseCase = deleteAllContactsUseCase,
            dispatcherProvider = TestDispatcherProvider(dispatcher, testScope),
            scope = testScope
        )
    }

    private fun setUserLevel(level: Int) {
        currentUserLevel = level
    }

    // --- setLogLoggingPin tests ---

    @Test
    fun `setLogLoggingPin stores PIN at correct level for user level 0`() {
        setUserLevel(0)
        pinComponent.setPin("1234")

        pinComponent.setLogLoggingPin("9999")

        val expectedLevel = PinLevels.logLoggingLevelFor(0)
        assertEquals(expectedLevel, pinManager.getPinLevel("9999"))
    }

    @Test
    fun `setLogLoggingPin stores PIN at correct level for duress user level 1`() {
        setUserLevel(1)
        pinComponent.setPin("1234")

        pinComponent.setLogLoggingPin("8888")

        val expectedLevel = PinLevels.logLoggingLevelFor(1)
        assertEquals(expectedLevel, pinManager.getPinLevel("8888"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setLogLoggingPin throws for hidden wallet level`() {
        setUserLevel(-1)

        pinComponent.setLogLoggingPin("7777")
    }

    // --- isLogLoggingPinSet tests ---

    @Test
    fun `isLogLoggingPinSet returns true when log logging PIN is set`() {
        setUserLevel(0)
        pinComponent.setPin("1234")

        assertFalse(pinComponent.isLogLoggingPinSet())

        pinComponent.setLogLoggingPin("5555")

        assertTrue(pinComponent.isLogLoggingPinSet())
    }

    @Test
    fun `isLogLoggingPinSet returns false when no log logging PIN is set`() {
        setUserLevel(0)
        pinComponent.setPin("1234")

        assertFalse(pinComponent.isLogLoggingPinSet())
    }

    @Test
    fun `isLogLoggingPinSet returns false for hidden wallet level`() {
        setUserLevel(-1)

        assertFalse(pinComponent.isLogLoggingPinSet())
    }

    @Test
    fun `isLogLoggingPinSet checks correct level for current user`() {
        // Set log logging PIN for user level 0
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("0000")

        assertTrue(pinComponent.isLogLoggingPinSet())

        // Switch to user level 1 - should not see level 0's log logging PIN
        setUserLevel(1)
        pinComponent.setPin("5678")

        assertFalse(pinComponent.isLogLoggingPinSet())
    }

    // --- disableLogLoggingPin tests ---

    @Test
    fun `disableLogLoggingPin removes log logging PIN for current user level`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("9999")

        assertTrue(pinComponent.isLogLoggingPinSet())

        pinComponent.disableLogLoggingPin()

        assertFalse(pinComponent.isLogLoggingPinSet())
        assertEquals(null, pinManager.getPinLevel("9999"))
    }

    @Test
    fun `disableLogLoggingPin does nothing for hidden wallet level`() {
        setUserLevel(-1)

        // Should not throw
        pinComponent.disableLogLoggingPin()

        assertFalse(pinComponent.isLogLoggingPinSet())
    }

    @Test
    fun `disableLogLoggingPin only affects current user level`() {
        // Set log logging PIN for user level 0
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("0000")

        // Set log logging PIN for user level 1
        setUserLevel(1)
        pinComponent.setPin("5678")
        pinComponent.setLogLoggingPin("1111")

        // Disable log logging PIN for level 1
        pinComponent.disableLogLoggingPin()

        assertFalse(pinComponent.isLogLoggingPinSet())

        // Switch back to level 0 - should still have log logging PIN
        setUserLevel(0)
        assertTrue(pinComponent.isLogLoggingPinSet())
    }

    // --- disableLogLoggingPinForDuress tests ---

    @Test
    fun `disableLogLoggingPinForDuress removes log logging PIN for duress level`() {
        setUserLevel(0)
        pinComponent.setPin("1234")

        // Set duress PIN
        pinComponent.setDuressPin("5678")

        // Switch to duress level and set log logging PIN
        setUserLevel(1)
        pinComponent.setLogLoggingPin("9999")
        assertTrue(pinComponent.isLogLoggingPinSet())

        // Switch back to main level
        setUserLevel(0)

        // Disable log logging PIN for duress
        pinComponent.disableLogLoggingPinForDuress()

        // Verify duress log logging PIN is removed
        setUserLevel(1)
        assertFalse(pinComponent.isLogLoggingPinSet())
    }

    // --- validateLogLoggingPin tests ---

    @Test
    fun `validateLogLoggingPin returns true for valid log logging PIN`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("9999")

        assertTrue(pinComponent.validateLogLoggingPin("9999"))
    }

    @Test
    fun `validateLogLoggingPin returns false for invalid PIN`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("9999")

        assertFalse(pinComponent.validateLogLoggingPin("0000"))
    }

    @Test
    fun `validateLogLoggingPin returns false for main PIN`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("9999")

        assertFalse(pinComponent.validateLogLoggingPin("1234"))
    }

    @Test
    fun `validateLogLoggingPin returns false for hidden wallet level`() {
        setUserLevel(-1)

        assertFalse(pinComponent.validateLogLoggingPin("1234"))
    }

    @Test
    fun `validateLogLoggingPin returns false for different user level log logging PIN`() {
        // Set log logging PIN for user level 0
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("0000")

        // Switch to user level 1
        setUserLevel(1)
        pinComponent.setPin("5678")

        // Level 0's log logging PIN should not validate at level 1
        assertFalse(pinComponent.validateLogLoggingPin("0000"))
    }

    // --- disablePin interaction with log logging PINs ---

    @Test
    fun `disablePin preserves current level log logging PIN`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("0000")  // Log logging for level 0

        assertTrue(pinComponent.isLogLoggingPinSet())

        pinComponent.disablePin()

        // Log logging PIN for level 0 should still exist
        assertTrue(pinComponent.isLogLoggingPinSet())
    }

    @Test
    fun `disablePin removes duress level log logging PIN`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setDuressPin("5678")

        // Set log logging PIN for duress level 1
        setUserLevel(1)
        pinComponent.setLogLoggingPin("1111")
        assertTrue(pinComponent.isLogLoggingPinSet())

        // Switch back and disable main PIN
        setUserLevel(0)
        pinComponent.disablePin()

        // Log logging PIN for duress level 1 should be removed
        setUserLevel(1)
        assertFalse(pinComponent.isLogLoggingPinSet())
    }

    // --- Integration tests ---

    @Test
    fun `log logging PIN and secure reset PIN can coexist`() {
        setUserLevel(0)
        pinComponent.setPin("1234")

        pinComponent.setLogLoggingPin("5555")
        pinComponent.setSecureResetPin("6666")

        assertTrue(pinComponent.isLogLoggingPinSet())
        assertTrue(pinComponent.isSecureResetPinSet())

        assertEquals(PinLevels.logLoggingLevelFor(0), pinManager.getPinLevel("5555"))
        assertEquals(PinLevels.SECURE_RESET, pinManager.getPinLevel("6666"))
    }

    @Test
    fun `multiple user levels can have independent log logging PINs`() {
        // Set log logging PIN for user level 0
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setLogLoggingPin("0000")

        // Set duress PIN and log logging PIN for level 1
        pinComponent.setDuressPin("5678")
        setUserLevel(1)
        pinComponent.setLogLoggingPin("1111")

        // Verify both exist at correct levels
        assertEquals(PinLevels.logLoggingLevelFor(0), pinManager.getPinLevel("0000"))
        assertEquals(PinLevels.logLoggingLevelFor(1), pinManager.getPinLevel("1111"))

        // Verify validation works for correct level
        setUserLevel(0)
        assertTrue(pinComponent.validateLogLoggingPin("0000"))
        assertFalse(pinComponent.validateLogLoggingPin("1111"))

        setUserLevel(1)
        assertTrue(pinComponent.validateLogLoggingPin("1111"))
        assertFalse(pinComponent.validateLogLoggingPin("0000"))
    }

    @Test
    fun `disableDuressPin keeps delete contacts PIN`() {
        setUserLevel(0)
        pinComponent.setPin("1234")
        pinComponent.setDeleteContactsPin("2222")
        pinComponent.setDuressPin("5678")

        setUserLevel(1)
        pinComponent.setLogLoggingPin("1111")

        setUserLevel(0)
        pinComponent.disableDuressPin()

        assertTrue(pinComponent.isDeleteContactsPinSet())
        assertEquals(PinLevels.DELETE_CONTACTS, pinManager.getPinLevel("2222"))
    }
}

private class LogLoggingTestPinDao : PinDao {
    private val pins = sortedMapOf<Int, Pin>()

    fun reset() = pins.clear()

    override fun insert(pin: Pin) {
        pins[pin.level] = pin
    }

    override fun get(level: Int): Pin? = pins[level]

    override fun getAll(): List<Pin> = pins.values.toList()

    override fun getLastLevelPin(): Pin? = pins.values
        .filter { PinLevels.isUserLevel(it.level) }
        .maxByOrNull { it.level }

    override fun deleteAllFromLevel(level: Int) {
        val iterator = pins.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().key >= level) {
                iterator.remove()
            }
        }
    }

    override fun deleteForLevel(level: Int) {
        pins.remove(level)
    }

    override fun deleteUserLevelsFromLevel(level: Int) {
        val iterator = pins.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next().key
            if (key >= level && key < PinLevels.SECURE_RESET) {
                iterator.remove()
            }
        }
    }

    override fun deleteLogLoggingPinsFromLevel(logLoggingLevel: Int) {
        val iterator = pins.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next().key
            if (key >= logLoggingLevel && key < PinLevels.DELETE_CONTACTS) {
                iterator.remove()
            }
        }
    }

    override fun getMinLevel(): Int? = pins.keys.minOrNull()
}
