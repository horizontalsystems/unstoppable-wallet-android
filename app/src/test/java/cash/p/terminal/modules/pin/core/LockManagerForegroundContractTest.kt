package cash.p.terminal.modules.pin.core

import android.content.Context
import android.content.SharedPreferences
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.calculator.domain.CalculatorAutoLockOption
import cash.p.terminal.modules.settings.security.autolock.AutoLockInterval
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks down the synchronous contract of [LockManager.willEnterForeground].
 *
 * MainActivity.onResume calls willEnterForeground() before consulting
 * isLockedFlow.value so the decision to dismiss the calculator overlay
 * never races the asynchronous BackgroundManager → PinComponent coroutine.
 * Each assertion below reads isLocked.value immediately after the call —
 * if any of these flips to asynchronous behavior, MainActivity's overlay
 * logic can briefly expose sensitive UI.
 */
class LockManagerForegroundContractTest {

    private val store = mutableMapOf<String, Any?>()
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private lateinit var pinManager: PinManager
    private lateinit var localStorage: ILocalStorage

    @Before
    fun setUp() {
        store.clear()

        editor = mockk(relaxed = true)
        every { editor.putLong(any(), any()) } answers {
            store[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg())
            editor
        }
        every { editor.apply() } just Runs
        every { editor.commit() } returns true

        prefs = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { prefs.getLong(any(), any()) } answers {
            (store[firstArg()] as? Long) ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            (store[firstArg()] as? Boolean) ?: secondArg()
        }

        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs

        pinManager = mockk(relaxed = true)
        localStorage = mockk(relaxed = true)
    }

    @Test
    fun willEnterForeground_generalModeElapsedExceedsInterval_locksSynchronously() {
        every { pinManager.isPinSet } returns true
        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.autoLockInterval } returns AutoLockInterval.AFTER_1_MIN
        store[KEY_LAST_BACKGROUND_TIME] = System.currentTimeMillis() - 2 * 60 * 1000L

        val lockManager = LockManager(pinManager, localStorage, context)
        lockManager.onUnlock()

        lockManager.willEnterForeground()

        assertTrue(
            "willEnterForeground() must lock synchronously once the auto-lock " +
                "interval has elapsed — MainActivity.onResume relies on this to " +
                "avoid dismissing the lock overlay during the BackgroundManager race.",
            lockManager.isLocked.value,
        )
    }

    @Test
    fun willEnterForeground_calculatorModeElapsedExceedsCalculatorOption_locksSynchronously() {
        every { pinManager.isPinSet } returns true
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorAutoLockOption } returns CalculatorAutoLockOption.AFTER_30_SEC
        every { localStorage.autoLockInterval } returns AutoLockInterval.AFTER_1_HOUR
        store[KEY_LAST_BACKGROUND_TIME] = System.currentTimeMillis() - 2 * 60 * 1000L

        val lockManager = LockManager(pinManager, localStorage, context)
        lockManager.onUnlock()

        lockManager.willEnterForeground()

        assertTrue(
            "In calculator mode the calculator option drives the lock decision; " +
                "after its interval elapses willEnterForeground() must lock " +
                "synchronously so MainActivity keeps the calculator overlay.",
            lockManager.isLocked.value,
        )
    }

    @Test
    fun willEnterForeground_pinNotSet_doesNotLockEvenWithStaleBackgroundTime() {
        every { pinManager.isPinSet } returns false
        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.autoLockInterval } returns AutoLockInterval.IMMEDIATE
        store[KEY_LAST_BACKGROUND_TIME] = System.currentTimeMillis() - 60 * 60 * 1000L

        val lockManager = LockManager(pinManager, localStorage, context)

        lockManager.willEnterForeground()

        assertFalse(
            "Without a PIN the lock state must remain false: a synchronous " +
                "willEnterForeground() call from onResume must not accidentally " +
                "lock an unprotected app.",
            lockManager.isLocked.value,
        )
    }

    @Test
    fun willEnterForeground_alreadyLocked_remainsLocked() {
        every { pinManager.isPinSet } returns true
        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.autoLockInterval } returns AutoLockInterval.AFTER_1_MIN

        val lockManager = LockManager(pinManager, localStorage, context)

        lockManager.willEnterForeground()

        assertTrue(
            "willEnterForeground() must be idempotent when already locked so " +
                "repeated onResume calls cannot flip the state.",
            lockManager.isLocked.value,
        )
    }

    companion object {
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
    }
}
