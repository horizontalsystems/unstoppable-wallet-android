package io.horizontalsystems.bankwallet.modules.welcome

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.core.ISystemInfoManager

class WelcomeInteractor(
        private val accountCreator: IAccountCreator,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val systemInfoManager: ISystemInfoManager) : WelcomeModule.IInteractor {

    var delegate: WelcomeModule.IInteractorDelegate? = null

    override val appVersion: String
        get() = systemInfoManager.appVersion

    override fun createWallet() {
        try {
            predefinedAccountTypeManager.allTypes.forEach { predefinedAccountType ->
                predefinedAccountType.defaultAccountType?.let {
                    accountCreator.createNewAccount(it)
                }
            }
            delegate?.didCreateWallet()

        } catch (ex: Exception) {
            delegate?.didFailToCreateWallet()
        }
    }

}
