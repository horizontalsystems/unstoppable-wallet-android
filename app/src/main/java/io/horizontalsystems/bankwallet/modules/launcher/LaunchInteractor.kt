package io.horizontalsystems.bankwallet.modules.launcher

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.security.KeyStoreValidationResult

class LaunchInteractor(
        private val accountManager: IAccountManager,
        private val pinComponent: IPinComponent,
        private val systemInfoManager: ISystemInfoManager,
        private val keyStoreManager: IKeyStoreManager,
        private val rootUtil: RootUtil,
        private val buildConfigProvider: IBuildConfigProvider,
        localStorage: ILocalStorage)
    : LaunchModule.IInteractor {

    var delegate: LaunchModule.IInteractorDelegate? = null

    override val isPinNotSet: Boolean
        get() = !pinComponent.isPinSet

    override val isAccountsEmpty: Boolean
        get() = accountManager.isAccountsEmpty

    override val isSystemLockOff: Boolean
        get() = systemInfoManager.isSystemLockOff

    override val isDeviceRooted: Boolean
        get() = rootUtil.isRooted()

    override val skipRootCheck: Boolean
        get() = buildConfigProvider.skipRootCheck

    override fun validateKeyStore(): KeyStoreValidationResult {
        return keyStoreManager.validateKeyStore()
    }

    override val mainShowedOnce: Boolean = localStorage.mainShowedOnce
}
