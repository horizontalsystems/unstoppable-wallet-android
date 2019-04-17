package io.horizontalsystems.bankwallet.modules.guest

import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.core.managers.AuthManager

class GuestInteractor(
        private val authManager: AuthManager,
        private val wordsManager: IWordsManager,
        private val keystoreSafeExecute: IKeyStoreSafeExecute,
        systemInfoManager: ISystemInfoManager) : GuestModule.IInteractor {

    var delegate: GuestModule.IInteractorDelegate? = null

    override val appVersion: String = systemInfoManager.appVersion

    override fun createWallet() {
        keystoreSafeExecute.safeExecute(
                action = Runnable { authManager.login(wordsManager.generateWords(), true) },
                onSuccess = Runnable { delegate?.didCreateWallet() },
                onFailure = Runnable { delegate?.didFailToCreateWallet() }
        )
    }

}
