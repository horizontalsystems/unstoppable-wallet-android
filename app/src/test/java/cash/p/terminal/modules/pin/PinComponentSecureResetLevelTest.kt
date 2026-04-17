package cash.p.terminal.modules.pin

import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.domain.usecase.DeleteAllContactsUseCase
import cash.p.terminal.modules.pin.core.Pin
import cash.p.terminal.modules.pin.core.PinDao
import cash.p.terminal.modules.pin.core.PinDbStorage
import cash.p.terminal.domain.usecase.ResetUseCase
import cash.p.terminal.modules.pin.core.PinLevels
import cash.p.terminal.modules.pin.core.PinManager
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.IPinSettingsStorage
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinComponentSecureResetLevelTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val pinDao = InMemoryPinDao()
    private val pinDbStorage = PinDbStorage(pinDao)
    private val pinManager = PinManager(pinDbStorage)
    private val userManager = mockk<DefaultUserManager>(relaxed = true)
    private val pinSettingsStorage = mockk<IPinSettingsStorage>(relaxed = true)
    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)
    private val resetUseCase = mockk<ResetUseCase>(relaxed = true)
    private val deleteAllContactsUseCase = mockk<DeleteAllContactsUseCase>(relaxed = true)
    private var secureResetCalled = false
    private var deleteContactsCalled = false
    private lateinit var pinComponent: PinComponent
    private var currentUserLevel = 0

    @Before
    fun setup() {
        pinDao.reset()
        currentUserLevel = 0

        // Mock App singleton
        mockkObject(App)
        every { App.localStorage } returns mockk<ILocalStorage>(relaxed = true)
        every { App.instance } returns mockk<CoreApp>(relaxed = true)

        every { userManager.getUserLevel() } answers { currentUserLevel }
        every { userManager.setUserLevel(any()) } answers {
            currentUserLevel = firstArg()
            Unit
        }
        every { userManager.currentUserLevelFlow } returns MutableStateFlow(currentUserLevel)

        clearMocks(resetUseCase)
        secureResetCalled = false
        deleteContactsCalled = false
        coEvery { resetUseCase.invoke() } answers {
            secureResetCalled = true
        }
        every { deleteAllContactsUseCase.invoke() } answers {
            deleteContactsCalled = true
        }

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

    @Test(expected = IllegalStateException::class)
    fun `duress PIN throws exception when user level is 9999`() {
        // Set user level to 9999
        setUserLevel(9999)

        // Attempting to set duress PIN should throw because duress level would be 10000 (SECURE_RESET)
        pinComponent.setDuressPin("1234")
    }

    @Test
    fun `duress PIN uses normal level when user level is not 9999`() {
        // Set user level to 0 (regular)
        setUserLevel(0)

        // Set duress PIN
        pinComponent.setDuressPin("5678")

        // Duress PIN should be at level 1
        val duressLevel = pinManager.getPinLevel("5678")
        assertEquals(1, duressLevel)
    }

    @Test
    fun `isDuressPinSet returns false when no duress PIN at correct level`() {
        setUserLevel(0)

        // Manually set a PIN at level 2 (not the expected duress level 1)
        pinManager.store("9999", 2)

        // isDuressPinSet should return false because duress should be at level 1
        assertFalse(pinComponent.isDuressPinSet())
    }

    @Test
    fun `isDuressPinSet returns true when duress PIN set at correct level`() {
        setUserLevel(0)

        // Set duress PIN through component
        pinComponent.setDuressPin("1111")

        // Should return true
        assertTrue(pinComponent.isDuressPinSet())
    }

    @Test
    fun `secure reset PIN and duress PIN can coexist`() {
        setUserLevel(0)

        // Set secure reset PIN
        pinComponent.setSecureResetPin("0000")

        // Set duress PIN
        pinComponent.setDuressPin("1111")

        // Both should exist at different levels
        assertEquals(PinLevels.SECURE_RESET, pinManager.getPinLevel("0000"))
        assertEquals(1, pinManager.getPinLevel("1111"))

        assertTrue(pinComponent.isSecureResetPinSet())
        assertTrue(pinComponent.isDuressPinSet())
    }

    @Test
    fun `delete contacts PIN uses reserved level`() {
        setUserLevel(0)

        pinComponent.setDeleteContactsPin("2222")

        assertTrue(pinComponent.isDeleteContactsPinSet())
        assertEquals(PinLevels.DELETE_CONTACTS, pinManager.getPinLevel("2222"))
    }

    @Test
    fun `disableDuressPin removes PIN from correct level`() {
        setUserLevel(0)

        // Set duress PIN
        pinComponent.setDuressPin("2222")
        assertTrue(pinComponent.isDuressPinSet())

        // Disable duress PIN
        pinComponent.disableDuressPin()

        // Should be disabled now
        assertFalse(pinComponent.isDuressPinSet())

        // PIN should not exist at level 1
        assertEquals(null, pinManager.getPinLevel("2222"))
    }

    @Test
    fun `secure reset pin promotes to primary pin on unlock`() = runTest(dispatcher) {
        setUserLevel(0)
        pinComponent.setPin("3333")
        pinComponent.setSecureResetPin("4444")

        val unlocked = pinComponent.unlock("4444", pinComponent.getPinLevel("4444"))

        assertTrue(unlocked)
        assertEquals(0, pinManager.getPinLevel("4444"))
        assertFalse(pinComponent.isSecureResetPinSet())
        assertEquals(0, currentUserLevel)
        assertTrue(secureResetCalled)
    }

    @Test
    fun `delete contacts pin clears contacts without unlocking`() = runTest(dispatcher) {
        setUserLevel(0)
        pinComponent.setPin("3333")
        pinComponent.setDeleteContactsPin("4444")

        val unlocked = pinComponent.unlock("4444", pinComponent.getPinLevel("4444"))

        assertFalse(unlocked)
        assertTrue(deleteContactsCalled)
        assertTrue(pinComponent.isDeleteContactsPinSet())
        assertEquals(PinLevels.DELETE_CONTACTS, pinManager.getPinLevel("4444"))
        assertEquals(0, currentUserLevel)
    }

    @Test
    fun `disablePin at duress level keeps delete contacts pin`() {
        setUserLevel(0)
        pinComponent.setPin("3333")
        pinComponent.setDeleteContactsPin("4444")
        pinComponent.setDuressPin("5555")

        setUserLevel(1)
        pinComponent.disablePin()

        assertEquals(0, pinManager.getPinLevel("3333"))
        assertEquals(null, pinManager.getPinLevel("5555"))
        assertTrue(pinComponent.isDeleteContactsPinSet())
        assertEquals(PinLevels.DELETE_CONTACTS, pinManager.getPinLevel("4444"))
    }

    @Test
    fun `disableSecureResetPin does not clear other pins`() {
        setUserLevel(0)

        // Set main PIN at level 0
        pinComponent.setPin("6666")

        // Set duress PIN at level 1
        pinComponent.setDuressPin("8888")
        assertTrue(pinComponent.isDuressPinSet())

        // Set secure reset PIN at level 10000
        pinComponent.setSecureResetPin("7777")
        assertTrue(pinComponent.isSecureResetPinSet())

        // Disable secure reset PIN
        pinComponent.disableSecureResetPin()

        // Secure reset PIN should be removed
        assertFalse(pinComponent.isSecureResetPinSet())
        assertEquals(null, pinManager.getPinLevel("7777"))

        // Main PIN and duress PIN should still exist
        assertEquals(0, pinManager.getPinLevel("6666"))
        assertTrue(pinComponent.isDuressPinSet())
        assertEquals(1, pinManager.getPinLevel("8888"))
    }

    @Test
    fun `disablePin removes secure reset PIN when exists`() {
        setUserLevel(0)

        // Set main PIN at level 0
        pinComponent.setPin("5555")
        assertEquals(0, pinManager.getPinLevel("5555"))

        // Set secure reset PIN at level 10000
        pinComponent.setSecureResetPin("6666")
        assertTrue(pinComponent.isSecureResetPinSet())

        // Disable main PIN (which clears all PINs above level 0)
        pinComponent.disablePin()

        // Main PIN should be cleared (passcode null but record exists)
        assertEquals(null, pinManager.getPinLevel("5555"))

        // Secure reset PIN should also be removed (level 10000 > level 0)
        assertFalse(pinComponent.isSecureResetPinSet())
        assertEquals(null, pinManager.getPinLevel("6666"))
    }

    @Test
    fun `disablePin removes delete contacts PIN when exists`() {
        setUserLevel(0)
        pinComponent.setPin("5555")
        pinComponent.setDeleteContactsPin("6666")

        pinComponent.disablePin()

        assertFalse(pinComponent.isDeleteContactsPinSet())
        assertEquals(null, pinManager.getPinLevel("6666"))
    }

    @Test
    fun `initDefaultPinLevel should restore user level from database after app restart`() {
        // Simulation: User has set PIN at level 0
        pinComponent.setPin("1234")
        assertEquals(0, pinManager.getPinLevel("1234"))
        assertEquals(0, currentUserLevel)

        // Simulate app restart:
        // 1. DefaultUserManager is recreated with currentUserLevel = Int.MAX_VALUE
        currentUserLevel = Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, currentUserLevel)

        // 2. On app start, initDefaultPinLevel() should be called
        // which restores the level from database
        pinComponent.initDefaultPinLevel()

        // EXPECTATION: currentUserLevel should be restored to 0
        assertEquals(0, currentUserLevel)
    }

    @Test
    fun `initDefaultPinLevel should restore correct user level for duress PIN scenario`() {
        // Simulation: User has set regular PIN (level 0) and duress PIN (level 1)
        pinComponent.setPin("1234")
        assertEquals(0, pinManager.getPinLevel("1234"))

        pinComponent.setDuressPin("5678")
        assertEquals(1, pinManager.getPinLevel("5678"))

        // User unlocked with duress PIN - level becomes 1
        setUserLevel(1)
        assertEquals(1, currentUserLevel)

        // Simulate app restart
        currentUserLevel = Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, currentUserLevel)

        // initDefaultPinLevel should restore the LAST used level
        // In this case it's level 1 (highest non-SECURE_RESET level)
        pinComponent.initDefaultPinLevel()

        // EXPECTATION: should restore to level 1 (last set PIN)
        assertEquals(1, currentUserLevel)
    }

    @Test
    fun `initDefaultPinLevel should work when no PIN is set`() {
        // Simulation: App launched for the first time, no PIN is set
        assertFalse(pinComponent.isPinSet)

        // currentUserLevel initialized as Int.MAX_VALUE
        currentUserLevel = Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, currentUserLevel)

        // Call initDefaultPinLevel when no PIN is set
        pinComponent.initDefaultPinLevel()

        // EXPECTATION: should be set to level 0 (default value from getPinLevelLast)
        assertEquals(0, currentUserLevel)
    }

    @Test
    fun `initDefaultPinLevel should skip SECURE_RESET level and restore highest user level`() {
        // Simulation: Regular PIN (level 0) and Secure Reset PIN (level 10000) are set
        pinComponent.setPin("1234")
        assertEquals(0, pinManager.getPinLevel("1234"))

        pinComponent.setSecureResetPin("9999")
        assertEquals(PinLevels.SECURE_RESET, pinManager.getPinLevel("9999"))
        assertTrue(pinComponent.isSecureResetPinSet())

        // Simulate app restart
        currentUserLevel = Int.MAX_VALUE

        // initDefaultPinLevel should restore level 0, ignoring SECURE_RESET (10000)
        pinComponent.initDefaultPinLevel()

        // EXPECTATION: currentUserLevel = 0, not 10000
        assertEquals(0, currentUserLevel)
    }
}

private class InMemoryPinDao : PinDao {
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
