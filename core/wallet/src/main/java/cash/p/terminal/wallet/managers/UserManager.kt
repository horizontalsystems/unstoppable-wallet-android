package cash.p.terminal.wallet.managers

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface UserManager {
    companion object {
        const val DEFAULT_USER_LEVEL = Int.MAX_VALUE
    }

    /** Current user level as state — replays the latest value to new subscribers. */
    val currentUserLevelFlow: StateFlow<Int>

    /**
     * Emits only when the user actively switches level (PIN unlock, biometric unlock, duress, lock).
     * Does NOT replay; does NOT emit during startup initialization via [initUserLevel].
     */
    val userLevelChangedFlow: SharedFlow<Int>

    fun getUserLevel(): Int

    /** User-initiated level change. Emits to [userLevelChangedFlow]. */
    fun setUserLevel(level: Int)

    /** Startup-time level initialization. Updates state but does NOT emit a change event. */
    fun initUserLevel(level: Int)

    fun allowAccountsForDuress(accountIds: List<String>)
    fun disallowAccountsForDuress()
}