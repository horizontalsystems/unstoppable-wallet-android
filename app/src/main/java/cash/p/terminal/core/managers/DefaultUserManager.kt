package cash.p.terminal.core.managers

import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.managers.UserManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DefaultUserManager(
    private val accountManager: IAccountManager
): UserManager {
    private var currentUserLevel = UserManager.DEFAULT_USER_LEVEL

    private val _currentUserLevelFlow = MutableStateFlow(currentUserLevel)
    override val currentUserLevelFlow: StateFlow<Int> = _currentUserLevelFlow.asStateFlow()

    private val _userLevelChangedFlow = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    override val userLevelChangedFlow: SharedFlow<Int> = _userLevelChangedFlow.asSharedFlow()

    override fun getUserLevel() = currentUserLevel

    override fun setUserLevel(level: Int) {
        val previousLevel = currentUserLevel
        if (!applyLevel(level)) return
        // Skip emission on cold-start login (DEFAULT_USER_LEVEL → real level).
        // It's not a switch between authenticated states; the back stack has nothing
        // to clear and popBackStack-driven listeners would only close a freshly opened
        // deeplink sheet.
        if (previousLevel == UserManager.DEFAULT_USER_LEVEL) return
        _userLevelChangedFlow.tryEmit(level)
    }

    override fun initUserLevel(level: Int) {
        applyLevel(level)
    }

    /** Applies new level to internal state and [currentUserLevelFlow]. Returns false if no-op. */
    private fun applyLevel(level: Int): Boolean {
        if (level == currentUserLevel) return false
        currentUserLevel = level
        accountManager.setLevel(level)          // Load accounts FIRST
        _currentUserLevelFlow.update { level }  // Then publish new state
        return true
    }

    override fun allowAccountsForDuress(accountIds: List<String>) {
        accountManager.updateAccountLevels(accountIds, currentUserLevel + 1)
    }

    override fun disallowAccountsForDuress() {
        accountManager.updateMaxLevel(currentUserLevel)
    }
}
