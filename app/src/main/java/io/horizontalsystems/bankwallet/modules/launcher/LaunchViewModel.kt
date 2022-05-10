package io.horizontalsystems.bankwallet.modules.launcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.security.KeyStoreValidationResult

class LaunchViewModel(private val interactor: LaunchInteractor) : ViewModel() {
    val openWelcomeModule = SingleLiveEvent<Void>()
    val openMainModule = SingleLiveEvent<Void>()
    val openUnlockModule = SingleLiveEvent<Void>()
    val openNoSystemLockModule = SingleLiveEvent<Void>()
    val openKeyInvalidatedModule = SingleLiveEvent<Void>()
    val openUserAuthenticationModule = SingleLiveEvent<Void>()
    val closeApplication = SingleLiveEvent<Void>()

    init {
        if (interactor.isSystemLockOff) {
            openNoSystemLockModule()
        } else {
            when (interactor.validateKeyStore()) {
                KeyStoreValidationResult.UserNotAuthenticated -> {
                    openUserAuthenticationModule()
                }
                KeyStoreValidationResult.KeyIsInvalid -> {
                    openKeyInvalidatedModule()
                }
                KeyStoreValidationResult.KeyIsValid -> {
                    when {
                        interactor.isAccountsEmpty && !interactor.mainShowedOnce -> openWelcomeModule()
                        interactor.isLocked -> openUnlockModule()
                        else -> openMainModule()
                    }
                }
            }
        }
    }

    private fun openWelcomeModule() {
        openWelcomeModule.call()
    }

    private fun openMainModule() {
        openMainModule.call()
    }

    private fun openUnlockModule() {
        openUnlockModule.call()
    }

    private fun openNoSystemLockModule() {
        openNoSystemLockModule.call()
    }

    private fun openKeyInvalidatedModule() {
        openKeyInvalidatedModule.call()
    }

    private fun openUserAuthenticationModule() {
        openUserAuthenticationModule.call()
    }

    private fun closeApplication() {
        closeApplication.call()
    }

    fun didUnlock() {
        openMainModule()
    }

    fun didCancelUnlock() {
        closeApplication()
    }
}
