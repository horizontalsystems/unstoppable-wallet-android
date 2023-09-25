package cash.p.terminal.core.managers

import cash.p.terminal.core.IAccountManager

class UserManager(
    private val accountManager: IAccountManager
) {
    private var currentUserLevel: Int? = null

    fun getUserLevel(): Int {
        return currentUserLevel!!
    }

    fun setUserLevel(level: Int) {
        currentUserLevel = level
        accountManager.setLevel(level)
    }

    fun allowAccountsForDuress(accountIds: List<String>) {
        accountManager.updateAccountLevels(accountIds, getUserLevel() + 1)
    }

    fun disallowAccountsForDuress() {
        accountManager.updateMaxLevel(getUserLevel())
    }
}
