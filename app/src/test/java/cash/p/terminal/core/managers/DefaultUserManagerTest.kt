package cash.p.terminal.core.managers

import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.managers.UserManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUserManagerTest {

    private val accountManager = mockk<IAccountManager>(relaxed = true).also {
        every { it.setLevel(any()) } returns Unit
    }

    private fun newManager() = DefaultUserManager(accountManager)

    @Test
    fun setUserLevel_fromDefaultUserLevel_doesNotEmit() = runTest {
        // Cold-start login: previous level is DEFAULT_USER_LEVEL (uninitialized).
        // This is a login event, not a switch between authenticated states.
        // No popBackStack-driven navigation must be triggered.
        val manager = newManager()
        val collected = mutableListOf<Int>()
        val job = launch { manager.userLevelChangedFlow.collect { collected += it } }
        advanceUntilIdle()

        manager.setUserLevel(1)
        advanceUntilIdle()

        assertTrue(
            "setUserLevel from DEFAULT_USER_LEVEL must NOT emit (cold-start login), got $collected",
            collected.isEmpty(),
        )
        job.cancel()
    }

    @Test
    fun setUserLevel_betweenTwoRealLevels_emits() = runTest {
        // Real switch between authenticated states (e.g. duress, auto-lock + unlock other PIN).
        val manager = newManager()
        manager.setUserLevel(0) // login from DEFAULT — silent

        val collected = mutableListOf<Int>()
        val job = launch { manager.userLevelChangedFlow.collect { collected += it } }
        advanceUntilIdle()

        manager.setUserLevel(1)
        advanceUntilIdle()

        assertEquals(listOf(1), collected)
        job.cancel()
    }

    @Test
    fun initUserLevel_doesNotEmitToUserLevelChangedFlow() = runTest {
        val manager = newManager()
        val collected = mutableListOf<Int>()
        val job = launch { manager.userLevelChangedFlow.collect { collected += it } }
        advanceUntilIdle()

        manager.initUserLevel(1)
        advanceUntilIdle()

        assertTrue("initUserLevel must not emit a change event, got $collected", collected.isEmpty())
        job.cancel()
    }

    @Test
    fun initUserLevel_updatesCurrentUserLevelFlowState() {
        val manager = newManager()

        manager.initUserLevel(2)

        assertEquals(2, manager.getUserLevel())
        assertEquals(2, manager.currentUserLevelFlow.value)
    }

    @Test
    fun initUserLevel_callsAccountManagerSetLevel() {
        val manager = newManager()

        manager.initUserLevel(3)

        verify { accountManager.setLevel(3) }
    }

    @Test
    fun initUserLevel_sameAsCurrent_isNoOp() = runTest {
        val manager = newManager()
        manager.initUserLevel(1)

        val collected = mutableListOf<Int>()
        val job = launch { manager.userLevelChangedFlow.collect { collected += it } }
        advanceUntilIdle()

        manager.initUserLevel(1) // Same level again
        advanceUntilIdle()

        assertTrue(collected.isEmpty())
        verify(exactly = 1) { accountManager.setLevel(1) }
        job.cancel()
    }

    @Test
    fun setUserLevel_sameAsCurrent_isNoOp() = runTest {
        val manager = newManager()
        manager.setUserLevel(1) // login from DEFAULT — silent
        manager.setUserLevel(0) // switch — would emit if not for the no-op below

        val collected = mutableListOf<Int>()
        val job = launch { manager.userLevelChangedFlow.collect { collected += it } }
        advanceUntilIdle()

        manager.setUserLevel(0) // same as current
        advanceUntilIdle()

        assertTrue(collected.isEmpty())
        job.cancel()
    }

    @Test
    fun currentUserLevelFlow_initialValueIsDefault() {
        val manager = newManager()
        assertEquals(UserManager.DEFAULT_USER_LEVEL, manager.currentUserLevelFlow.value)
        assertEquals(UserManager.DEFAULT_USER_LEVEL, manager.getUserLevel())
    }

    @Test
    fun setUserLevel_afterInitUserLevel_emitsOnlyTheChange() = runTest {
        val manager = newManager()
        manager.initUserLevel(1) // Silent

        val collected = mutableListOf<Int>()
        val job = launch { manager.userLevelChangedFlow.collect { collected += it } }
        advanceUntilIdle()

        manager.setUserLevel(0) // Real user-initiated change
        advanceUntilIdle()

        assertEquals(listOf(0), collected)
        job.cancel()
    }
}
