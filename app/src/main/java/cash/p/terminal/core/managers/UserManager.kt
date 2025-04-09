package cash.p.terminal.core.managers

import cash.p.terminal.wallet.IAccountManager
import io.horizontalsystems.core.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserManager(
    private val accountManager: IAccountManager
) {
    private val logger: AppLogger = AppLogger("UserManager")

    private var currentUserLevel = Int.MAX_VALUE

    private val _currentUserLevelFlow = MutableStateFlow(currentUserLevel)
    val currentUserLevelFlow: StateFlow<Int>
        get() = _currentUserLevelFlow.asStateFlow()

    fun getUserLevel() = currentUserLevel

    fun setUserLevel(level: Int) {
        if (level == currentUserLevel) {
            logger.info("User level is already set to $level")
            return
        }

        currentUserLevel = level
        _currentUserLevelFlow.update { level }
        accountManager.setLevel(level)
    }

    fun allowAccountsForDuress(accountIds: List<String>) {
        accountManager.updateAccountLevels(accountIds, currentUserLevel + 1)
    }

    fun disallowAccountsForDuress() {
        accountManager.updateMaxLevel(currentUserLevel)
    }
}
