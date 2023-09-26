package cash.p.terminal.core.managers

import cash.p.terminal.core.IAccountManager

class UserManager(
    private val accountManager: IAccountManager
) {
    private var currentUserLevel = Int.MAX_VALUE

    fun getUserLevel() = currentUserLevel

    fun setUserLevel(level: Int) {
        currentUserLevel = level
        accountManager.setLevel(level)
    }

    fun allowAccountsForDuress(accountIds: List<String>) {
        accountManager.updateAccountLevels(accountIds, currentUserLevel + 1)
    }

    fun disallowAccountsForDuress() {
        accountManager.updateMaxLevel(currentUserLevel)
    }
}
