package io.horizontalsystems.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackgroundManagerTest {

    private lateinit var application: Application
    private lateinit var backgroundManager: BackgroundManager

    @Before
    fun setUp() {
        application = mockk(relaxed = true)
        backgroundManager = BackgroundManager(application)
    }

    private fun mockActivity(destroyed: Boolean = false): AppCompatActivity =
        mockk(relaxed = true) {
            every { isDestroyed } returns destroyed
        }

    private fun awaitState(expected: BackgroundManagerState) = runBlocking {
        withTimeout(1_000) {
            backgroundManager.stateFlow.first { it == expected }
        }
    }

    @Test
    fun constructor_registersLifecycleCallbacks() {
        verify { application.registerActivityLifecycleCallbacks(backgroundManager) }
    }

    @Test
    fun currentActivity_initialState_returnsNull() {
        assertNull(backgroundManager.currentActivity)
    }

    @Test
    fun currentActivity_afterSingleResume_returnsActivity() {
        val activity = mockActivity()

        backgroundManager.onActivityResumed(activity)

        assertSame(activity, backgroundManager.currentActivity)
    }

    /** Regression for PR #289 — currentActivity must return the most recently resumed activity, not a random non-destroyed one. */
    @Test
    fun currentActivity_afterTwoResumes_returnsLastResumed() {
        val first = mockActivity()
        val second = mockActivity()

        backgroundManager.onActivityResumed(first)
        backgroundManager.onActivityResumed(second)

        assertSame(second, backgroundManager.currentActivity)
    }

    @Test
    fun currentActivity_afterLastResumedDestroyed_returnsNull() {
        val activity = mockActivity()
        backgroundManager.onActivityCreated(activity, null)
        backgroundManager.onActivityResumed(activity)

        backgroundManager.onActivityDestroyed(activity)

        assertNull(backgroundManager.currentActivity)
    }

    @Test
    fun currentActivity_whenActivityFlagsDestroyed_returnsNull() {
        val activity = mockActivity(destroyed = false)
        backgroundManager.onActivityResumed(activity)
        assertSame(activity, backgroundManager.currentActivity)

        every { activity.isDestroyed } returns true

        assertNull(backgroundManager.currentActivity)
    }

    @Test
    fun currentActivity_nonAppCompatActivityResumed_returnsNull() {
        val plainActivity = mockk<Activity>(relaxed = true)

        backgroundManager.onActivityResumed(plainActivity)

        assertNull(backgroundManager.currentActivity)
    }

    @Test
    fun currentActivity_otherActivityDestroyed_stillReturnsLastResumed() {
        val first = mockActivity()
        val second = mockActivity()
        backgroundManager.onActivityCreated(first, null)
        backgroundManager.onActivityCreated(second, null)
        backgroundManager.onActivityResumed(first)
        backgroundManager.onActivityResumed(second)

        backgroundManager.onActivityDestroyed(first)

        assertSame(second, backgroundManager.currentActivity)
    }

    @Test
    fun inForeground_initialState_isFalse() {
        assertFalse(backgroundManager.inForeground)
    }

    @Test
    fun inForeground_afterStart_isTrue() {
        backgroundManager.onActivityStarted(mockActivity())
        assertTrue(backgroundManager.inForeground)
    }

    @Test
    fun inForeground_afterStartAndStop_isFalse() {
        val activity = mockActivity()
        backgroundManager.onActivityStarted(activity)
        backgroundManager.onActivityStopped(activity)

        assertFalse(backgroundManager.inForeground)
    }

    @Test
    fun inForeground_twoStartsOneStop_remainsTrue() {
        val first = mockActivity()
        val second = mockActivity()
        backgroundManager.onActivityStarted(first)
        backgroundManager.onActivityStarted(second)

        backgroundManager.onActivityStopped(first)

        assertTrue(backgroundManager.inForeground)
    }

    @Test
    fun stateFlow_onFirstStart_emitsEnterForeground() {
        backgroundManager.onActivityStarted(mockActivity())

        assertEquals(BackgroundManagerState.EnterForeground, awaitState(BackgroundManagerState.EnterForeground))
    }

    @Test
    fun stateFlow_whenAllStopped_emitsEnterBackground() {
        val activity = mockActivity()
        backgroundManager.onActivityStarted(activity)
        awaitState(BackgroundManagerState.EnterForeground)

        backgroundManager.onActivityStopped(activity)

        assertEquals(BackgroundManagerState.EnterBackground, awaitState(BackgroundManagerState.EnterBackground))
    }

    @Test
    fun stateFlow_whenAllDestroyed_emitsAllActivitiesDestroyed() {
        val activity = mockActivity()
        backgroundManager.onActivityCreated(activity, null)

        backgroundManager.onActivityDestroyed(activity)

        assertEquals(
            BackgroundManagerState.AllActivitiesDestroyed,
            awaitState(BackgroundManagerState.AllActivitiesDestroyed)
        )
    }

    @Test
    fun onBeforeEnterBackground_invokedWhenAllStopped() {
        var called = false
        backgroundManager.onBeforeEnterBackground = { called = true }
        val activity = mockActivity()
        backgroundManager.onActivityStarted(activity)
        awaitState(BackgroundManagerState.EnterForeground)

        backgroundManager.onActivityStopped(activity)
        awaitState(BackgroundManagerState.EnterBackground)

        assertTrue(called)
    }

    @Test
    fun stateFlow_secondStartAfterStop_emitsEnterForegroundAgain() {
        val activity = mockActivity()
        backgroundManager.onActivityStarted(activity)
        awaitState(BackgroundManagerState.EnterForeground)
        backgroundManager.onActivityStopped(activity)
        awaitState(BackgroundManagerState.EnterBackground)

        backgroundManager.onActivityStarted(activity)

        assertEquals(BackgroundManagerState.EnterForeground, awaitState(BackgroundManagerState.EnterForeground))
    }

    @Test
    fun onActivitySaveInstanceState_doesNotThrow() {
        val activity = mockActivity()
        val outState = mockk<Bundle>(relaxed = true)

        backgroundManager.onActivitySaveInstanceState(activity, outState)
    }

    @Test
    fun onActivityPaused_doesNotAffectCurrentActivity() {
        val activity = mockActivity()
        backgroundManager.onActivityResumed(activity)

        backgroundManager.onActivityPaused(activity)

        assertSame(activity, backgroundManager.currentActivity)
    }
}
