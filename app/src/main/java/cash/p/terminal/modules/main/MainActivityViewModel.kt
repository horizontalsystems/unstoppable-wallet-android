package cash.p.terminal.modules.main

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.DAppRequestEntityWrapper
import cash.p.terminal.core.managers.DefaultUserManager
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.modules.calculator.domain.CalculatorModeService
import cash.p.terminal.modules.walletconnect.WCDelegate
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.managers.UserManager
import com.reown.walletkit.client.Wallet
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ILoginRecordRepository
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.entities.AutoDeletePeriod
import io.horizontalsystems.core.security.KeyStoreValidationError
import io.horizontalsystems.tonkit.models.SignTransaction
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivityViewModel(
    private val userManager: DefaultUserManager,
    private val accountManager: IAccountManager,
    private val systemInfoManager: ISystemInfoManager,
    private val localStorage: ILocalStorage,
    private val checkPremiumUseCase: CheckPremiumUseCase,
    private val calculatorModeService: CalculatorModeService,
    private val dispatcherProvider: DispatcherProvider,
    pinComponent: IPinComponent,
    private val keyStoreManager: IKeyStoreManager,
    private val tonConnectManager: TonConnectManager,
    private val loginRecordRepository: ILoginRecordRepository,
) : ViewModel() {

    val isLockedFlow = pinComponent.isLockedFlow

    val navigateToMainLiveData = MutableLiveData(false)
    val wcEvent = MutableLiveData<Wallet.Model?>()

    private val _tcSendRequest = MutableSharedFlow<SignTransaction?>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val tcSendRequest: SharedFlow<SignTransaction?> = _tcSendRequest.asSharedFlow()


    val tcDappRequest = MutableLiveData<DAppRequestEntityWrapper?>()
    val intentLiveData = MutableLiveData<Intent?>()

    init {
        viewModelScope.launch {
            combine(userManager.currentUserLevelFlow, isLockedFlow) { level, isLocked ->
                level.takeIf { it != UserManager.DEFAULT_USER_LEVEL && !isLocked }
            }.distinctUntilChanged()
                .filterNotNull()
                .collect { level ->
                    refreshUserLevelScopedState(level)
                }
        }
        viewModelScope.launch {
            // Only real user-initiated level changes pop to main.
            // Startup-time level initialization doesn't emit here, so it can't close deeplink sheets.
            userManager.userLevelChangedFlow.collect {
                navigateToMainLiveData.postValue(true)
            }
        }
        viewModelScope.launch {
            WCDelegate.walletEvents.collect {
                wcEvent.postValue(it)
            }
        }
        viewModelScope.launch {
            tonConnectManager.sendRequestFlow.collect {
                _tcSendRequest.emit(it)
            }
        }
        viewModelScope.launch {
            tonConnectManager.dappRequestFlow.collect {
                tcDappRequest.postValue(it)
            }
        }
    }

    private fun refreshUserLevelScopedState(level: Int) {
        updatePremiumStatus()
        cleanupExpiredLoginRecords(level)
    }

    private fun updatePremiumStatus() {
        viewModelScope.launch(
            dispatcherProvider.io + CoroutineExceptionHandler { _, exception ->
                Timber.e(exception, "Failed to refresh premium status")
            }
        ) {
            val premiumType = checkPremiumUseCase.update()
            if (!premiumType.isPremium() && localStorage.isCalculatorModeEnabled) {
                calculatorModeService.disableAfterPremiumLoss()
            }
        }
    }

    private fun cleanupExpiredLoginRecords(level: Int) {
        val period = AutoDeletePeriod.fromValue(localStorage.getAutoDeleteLogsPeriod(level))
        if (period != AutoDeletePeriod.NEVER) {
            viewModelScope.launch {
                loginRecordRepository.deleteExpired(level, period)
            }
        }
    }

    fun setIntent(intent: Intent) {
        intentLiveData.postValue(intent)
    }

    fun intentHandled() {
        intentLiveData.postValue(null)
    }

    fun onTcDappRequestHandled() {
        tcDappRequest.postValue(null)
    }

    fun onWcEventHandled() {
        wcEvent.postValue(null)
    }

    fun onTcSendRequestHandled() {
        _tcSendRequest.tryEmit(null)
    }

    fun validate() {
        if (systemInfoManager.isSystemLockOff) {
            throw MainScreenValidationError.NoSystemLock()
        }

        try {
            keyStoreManager.validateKeyStore()
        } catch (e: KeyStoreValidationError.UserNotAuthenticated) {
            throw MainScreenValidationError.UserAuthentication()
        } catch (e: KeyStoreValidationError.KeyIsInvalid) {
            throw MainScreenValidationError.KeyInvalidated()
        } catch (e: RuntimeException) {
            throw MainScreenValidationError.KeystoreRuntimeException()
        }

        if (accountManager.isAccountsEmpty && !localStorage.mainShowedOnce) {
            throw MainScreenValidationError.Welcome()
        }
    }

    fun onNavigatedToMain() {
        navigateToMainLiveData.postValue(false)
    }

    fun selectBalanceTabOnNextLaunch() {
        localStorage.selectBalanceTabOnNextLaunch = true
    }
}

sealed class MainScreenValidationError : Exception() {
    class Welcome : MainScreenValidationError()
    class NoSystemLock : MainScreenValidationError()
    class KeyInvalidated : MainScreenValidationError()
    class UserAuthentication : MainScreenValidationError()
    class KeystoreRuntimeException : MainScreenValidationError()
}
