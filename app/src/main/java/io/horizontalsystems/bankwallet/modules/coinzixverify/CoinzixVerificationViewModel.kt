package cash.p.terminal.modules.coinzixverify

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.CoinzixCexProvider
import cash.p.terminal.modules.balance.cex.CoinzixCexApiService
import kotlinx.coroutines.launch

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

    var uiState by mutableStateOf(
        CoinzixVerificationUiState(
            submitEnabled = getSubmitEnabled(),
            emailCodeEnabled = emailCodeRequired,
            googleCodeEnabled = googleCodeRequired,
            success = success,
            loading = loading,
            error = error
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

    private fun emitState() {
        viewModelScope.launch {
            uiState = CoinzixVerificationUiState(
                submitEnabled = getSubmitEnabled(),
                emailCodeEnabled = emailCodeRequired,
                googleCodeEnabled = googleCodeRequired,
                success = success,
                loading = loading,
                error = error
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

                val tmpEmailCode = emailCode

                if (tmpEmailCode == null) {
                    throw IllegalStateException()
                } else {
                    verifyService.verify(tmpEmailCode, twoFactorCode)

                    success = true
                }
            } catch (err: Throwable) {
                loading = false
                error = err
            }

            emitState()
        }
    }

    fun onResendEmailCode() {
        viewModelScope.launch {
            try {
                verifyService.resendPin()
            } catch (err: Throwable) {
                error = err
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
)
