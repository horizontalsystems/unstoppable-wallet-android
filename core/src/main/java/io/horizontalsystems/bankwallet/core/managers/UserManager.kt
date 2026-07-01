package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserManager(
    private val accountManager: IAccountManager
) {
    private var currentUserLevel = Int.MAX_VALUE

    private val _currentUserLevelFlow = MutableStateFlow(currentUserLevel)
    val currentUserLevelFlow: StateFlow<Int>
        get() = _currentUserLevelFlow.asStateFlow()

    fun getUserLevel() = currentUserLevel

    fun setUserLevel(level: Int) {
        if (level == currentUserLevel) return

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
