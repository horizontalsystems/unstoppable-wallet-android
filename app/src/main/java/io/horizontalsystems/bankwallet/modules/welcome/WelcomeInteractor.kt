package io.horizontalsystems.bankwallet.modules.welcome

import io.horizontalsystems.core.ISystemInfoManager

class WelcomeInteractor(private val systemInfoManager: ISystemInfoManager) : WelcomeModule.IInteractor {

    override val appVersion: String
        get() = systemInfoManager.appVersion
}
