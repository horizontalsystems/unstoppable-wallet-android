package io.horizontalsystems.bankwallet.modules.withdrawcex.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.ICexProvider
import kotlinx.coroutines.launch

class CexWithdrawVerificationViewModel(
    private val withdrawId: String,
    private val cexProvider: ICexProvider?
) : ViewModel() {
    private var emailCode: String? = null
    private var twoFactorCode: String? = null

    private var success = false
    private var error: Throwable? = null

    var uiState by mutableStateOf(
        CexWithdrawVerificationUiState(
            submitEnabled = getSubmitEnabled(),
            success = success,
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
            uiState = CexWithdrawVerificationUiState(
                submitEnabled = getSubmitEnabled(),
                success = success,
                error = error
            )
        }
    }

    private fun getSubmitEnabled(): Boolean {
        return !emailCode.isNullOrBlank() && !twoFactorCode.isNullOrBlank()
    }

    fun submit() {
        viewModelScope.launch {
            try {
                val tmpEmailCode = emailCode
                val tmpCexProvider = cexProvider

                if (tmpEmailCode == null || tmpCexProvider == null) {
                    throw IllegalStateException()
                } else {
                    tmpCexProvider.confirmWithdraw(withdrawId, tmpEmailCode, twoFactorCode)
                    success = true
                }
            } catch (err: Throwable) {
                error = err
            }

            emitState()
        }
    }

    fun onResendEmailVerificationCode() {
        viewModelScope.launch {
            try {
                val provider = cexProvider ?: throw IllegalStateException()
                provider.sendWithdrawPin(withdrawId)
            } catch (err: Throwable) {
                error = err
            }

            emitState()
        }
    }

    class Factory(private val withdrawId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val cexProvider = App.cexProviderManager.cexProviderFlow.value

            return CexWithdrawVerificationViewModel(withdrawId, cexProvider) as T
        }
    }
}

data class CexWithdrawVerificationUiState(
    val submitEnabled: Boolean,
    val success: Boolean,
    val error: Throwable?
)
