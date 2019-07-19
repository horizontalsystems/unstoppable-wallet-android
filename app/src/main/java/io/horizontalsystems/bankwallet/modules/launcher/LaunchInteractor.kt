package io.horizontalsystems.bankwallet.modules.launcher

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IPinManager

class LaunchInteractor(private val accountManager: IAccountManager,
                       private val pinManager: IPinManager) : LaunchModule.IInteractor {

    var delegate: LaunchModule.IInteractorDelegate? = null

    override val isPinNotSet: Boolean
        get() = !pinManager.isPinSet

    override val isAccountsEmpty: Boolean
        get() = accountManager.accounts.isEmpty()

    override val isDeviceLockDisabled: Boolean
        get() = !pinManager.isDeviceLockEnabled

}
