package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager

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

    fun makeAccountsAvailableInDuress(accountIds: List<String>) {
        accountManager.updateAccountLevels(accountIds, getUserLevel() + 1)
    }
}
