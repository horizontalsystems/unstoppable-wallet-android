package io.horizontalsystems.bankwallet.modules.coinzixverify

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.CoinzixCexProvider
import io.horizontalsystems.bankwallet.modules.balance.cex.CoinzixCexApiService
import io.horizontalsystems.bankwallet.modules.coinzixverify.ui.CodeGetButtonState
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.schedule

class CoinzixVerificationViewModel(
    twoFactorTypes: List<TwoFactorType>,
    private val verifyService: ICoinzixVerifyService
) : ViewModel() {

    private var emailCode: String? = null
    private var twoFactorCode: String? = null

    private var success = false
    private var loading = false
    private var error: Throwable? = null

    private val emailCodeRequired = twoFactorTypes.contains(TwoFactorType.Email)
    private val googleCodeRequired = twoFactorTypes.contains(TwoFactorType.Authenticator)

    private var pinResendTimeIntervalInSeconds = 5
    private var pinResendTimeout = pinResendTimeIntervalInSeconds
    private var resendButtonState: CodeGetButtonState = CodeGetButtonState.Pending(pinResendTimeIntervalInSeconds)

    private var timer: Timer? = null

    init {
        startTimer()
    }

    var uiState by mutableStateOf(
        CoinzixVerificationUiState(
            submitEnabled = getSubmitEnabled(),
            emailCodeEnabled = emailCodeRequired,
            googleCodeEnabled = googleCodeRequired,
            success = success,
            loading = loading,
            error = error,
            resendButtonState = resendButtonState
        )
    )
        private set

    fun onEnterEmailCode(v: String) {
        emailCode = v

        emitState()
    }

    fun onEnterTwoFactorCode(v: String) {
        twoFactorCode = v

        emitState()
    }

    private fun startTimer() {
        pinResendTimeout = pinResendTimeIntervalInSeconds

        timer = Timer().apply {
            schedule(1000, 1000) {
                onFireTimer()
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun resetTimer() {
        stopTimer()
        startTimer()
    }

    private fun onFireTimer() {
        pinResendTimeout = pinResendTimeout.dec().coerceAtLeast(0)

        if (pinResendTimeout == 0) {
            stopTimer()
        }

        emitState()
    }

    private fun emitState() {
        resendButtonState = if (pinResendTimeout > 0) {
            CodeGetButtonState.Pending(pinResendTimeout)
        } else {
            CodeGetButtonState.Active
        }

        viewModelScope.launch {
            uiState = CoinzixVerificationUiState(
                submitEnabled = getSubmitEnabled(),
                emailCodeEnabled = emailCodeRequired,
                googleCodeEnabled = googleCodeRequired,
                success = success,
                loading = loading,
                error = error,
                resendButtonState = resendButtonState
            )
        }
    }

    private fun getSubmitEnabled(): Boolean {
        return !loading && (!emailCodeRequired || !emailCode.isNullOrBlank()) && (!googleCodeRequired || !twoFactorCode.isNullOrBlank())
    }

    fun submit() {
        viewModelScope.launch {
            try {
                loading = true
                emitState()

                verifyService.verify(emailCode, twoFactorCode)

                success = true
            } catch (err: Throwable) {
                loading = false
                error = err
            }

            emitState()
        }
    }

    fun onResendEmailCode() {
        viewModelScope.launch {
            resetTimer()
            emitState()

            try {
                verifyService.resendPin()
            } catch (err: Throwable) {
                error = err

                stopTimer()
                pinResendTimeout = 0
            }

            emitState()
        }
    }

    class FactoryForWithdraw(
        private val withdrawId: String,
        private val twoFactorTypes: List<TwoFactorType>,
        private val provider: CoinzixCexProvider
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val verifyService = WithdrawCoinzixVerifyService(provider, withdrawId)
            return CoinzixVerificationViewModel(twoFactorTypes, verifyService) as T
        }
    }

    class FactoryForLogin(
        private val token: String,
        private val secret: String,
        private val twoFactorTypes: List<TwoFactorType>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val verifyService = LoginCoinzixVerifyService(token, secret, CoinzixCexApiService(), App.accountManager, App.accountFactory)
            return CoinzixVerificationViewModel(twoFactorTypes, verifyService) as T
        }
    }
}

data class CoinzixVerificationUiState(
    val emailCodeEnabled: Boolean,
    val googleCodeEnabled: Boolean,
    val submitEnabled: Boolean,
    val success: Boolean,
    val loading: Boolean,
    val error: Throwable?,
    val resendButtonState: CodeGetButtonState
)
