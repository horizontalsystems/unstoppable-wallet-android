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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Red tests for MOBILE-682.
 *
 * The user-facing "Auto-Lock" setting in calculator pin settings
 * (CalculatorAutoLockOption) must determine how long the app may stay
 * unlocked in the background while calculator mode is active. Today
 * LockManager only ever consults the general AutoLockInterval, so the
 * calculator timer is silently ignored and the app re-locks immediately
 * on any pause/resume cycle.
 */
class LockManagerCalculatorAutoLockTest {

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
        every { pinManager.isPinSet } returns true

        localStorage = mockk(relaxed = true)
    }

    @Test
    fun calculatorMode_briefBackground_doesNotLockBeforeCalculatorTimer() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorAutoLockOption } returns CalculatorAutoLockOption.AFTER_2_MIN
        // The general AutoLockInterval defaults to a short value or even IMMEDIATE,
        // which today wins because LockManager ignores the calculator setting.
        every { localStorage.autoLockInterval } returns AutoLockInterval.IMMEDIATE

        val lockManager = LockManager(pinManager, localStorage, context)
        lockManager.onUnlock()

        lockManager.didEnterBackground()
        lockManager.willEnterForeground()

        assertFalse(
            "Calculator mode with a 2-minute auto-lock must not re-lock instantly " +
                "when the user returns within the configured interval.",
            lockManager.isLocked.value,
        )
    }

    @Test
    fun calculatorMode_immediateBackground_doesNotLockEvenIfGeneralIntervalIsImmediate() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorAutoLockOption } returns CalculatorAutoLockOption.AFTER_30_SEC
        every { localStorage.autoLockInterval } returns AutoLockInterval.IMMEDIATE

        val lockManager = LockManager(pinManager, localStorage, context)
        lockManager.onUnlock()

        lockManager.didEnterBackground()
        lockManager.willEnterForeground()

        assertFalse(
            "Calculator's own auto-lock option must override the general AutoLockInterval " +
                "while calculator mode is active.",
            lockManager.isLocked.value,
        )
    }

    @Test
    fun calculatorMode_backgroundOnly_doesNotLockImmediately() {
        every { localStorage.isCalculatorModeEnabled } returns true
        every { localStorage.calculatorAutoLockOption } returns CalculatorAutoLockOption.AFTER_2_MIN
        every { localStorage.autoLockInterval } returns AutoLockInterval.AFTER_1_MIN

        val lockManager = LockManager(pinManager, localStorage, context)
        lockManager.onUnlock()

        lockManager.didEnterBackground()

        assertFalse(
            "Going to background alone must not lock the app: locking should only happen " +
                "after the calculator auto-lock interval has elapsed.",
            lockManager.isLocked.value,
        )
    }

    @Test
    fun normalMode_immediateInterval_locksAsBefore() {
        every { localStorage.isCalculatorModeEnabled } returns false
        every { localStorage.autoLockInterval } returns AutoLockInterval.IMMEDIATE

        val lockManager = LockManager(pinManager, localStorage, context)
        lockManager.onUnlock()

        lockManager.didEnterBackground()
        lockManager.willEnterForeground()

        assertEquals(
            "Non-calculator behavior must remain untouched: IMMEDIATE general interval " +
                "still re-locks the app on resume.",
            true,
            lockManager.isLocked.value,
        )
    }
}
