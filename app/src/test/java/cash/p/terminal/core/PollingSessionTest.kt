package cash.p.terminal.core

import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PollingSessionTest {

    private fun backgroundManager(state: BackgroundManagerState) = mockk<BackgroundManager> {
        every { stateFlow } returns MutableStateFlow(state)
    }

    // onPollingStarted

    @Test
    fun onPollingStarted_callsActionAndIncrementsCounter() {
        val counter = AtomicInteger(0)
        var called = false

        counter.onPollingStarted { called = true }

        assertTrue(called)
        assertEquals(1, counter.get())
    }

    @Test
    fun onPollingStarted_actionThrows_decrementsAndRethrows() {
        val counter = AtomicInteger(0)
        var caught = false

        try {
            counter.onPollingStarted { throw RuntimeException("fail") }
        } catch (e: RuntimeException) {
            caught = true
        }

        assertTrue(caught)
        assertEquals(0, counter.get())
    }

    // onPollingStopped — foreground guard

    @Test
    fun onPollingStopped_inBackground_callsAction() {
        val counter = AtomicInteger(1)
        var stopped = false

        counter.onPollingStopped(backgroundManager(BackgroundManagerState.EnterBackground)) {
            stopped = true
        }

        assertTrue(stopped)
        assertEquals(0, counter.get())
    }

    @Test
    fun onPollingStopped_inForeground_skipsAction() {
        val counter = AtomicInteger(1)
        var stopped = false

        counter.onPollingStopped(backgroundManager(BackgroundManagerState.EnterForeground)) {
            stopped = true
        }

        assertFalse(stopped, "Kit must not be stopped when app returned to foreground")
        assertEquals(0, counter.get())
    }

    @Test
    fun onPollingStopped_unknownState_callsAction() {
        val counter = AtomicInteger(1)
        var stopped = false

        counter.onPollingStopped(backgroundManager(BackgroundManagerState.Unknown)) {
            stopped = true
        }

        assertTrue(stopped)
        assertEquals(0, counter.get())
    }

    @Test
    fun onPollingStopped_actionThrows_stillDecrements() {
        val counter = AtomicInteger(1)

        try {
            counter.onPollingStopped(backgroundManager(BackgroundManagerState.EnterBackground)) {
                throw RuntimeException("fail")
            }
        } catch (_: RuntimeException) {
        }

        assertEquals(0, counter.get())
    }

    // suspend variants

    @Test
    fun onPollingStartedSuspend_actionThrows_decrementsAndRethrows() = runTest {
        val counter = AtomicInteger(0)
        var caught = false

        try {
            counter.onPollingStartedSuspend { throw RuntimeException("fail") }
        } catch (e: RuntimeException) {
            caught = true
        }

        assertTrue(caught)
        assertEquals(0, counter.get())
    }

    @Test
    fun onPollingStoppedSuspend_inForeground_skipsAction() = runTest {
        val counter = AtomicInteger(1)
        var stopped = false

        counter.onPollingStoppedSuspend(backgroundManager(BackgroundManagerState.EnterForeground)) {
            stopped = true
        }

        assertFalse(stopped, "Kit must not be stopped when app returned to foreground")
        assertEquals(0, counter.get())
    }
}
