package cash.p.terminal.modules.importcexaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.CexType
import cash.p.terminal.modules.balance.cex.CoinzixCexApiService
import kotlinx.coroutines.launch

class EnterCexDataCoinzixViewModel : ViewModel() {
    private val accountManager = App.accountManager
    private val accountFactory = App.accountFactory
    private val coinzixCexApiService = CoinzixCexApiService()

    private var email: String? = null
    private var password: String? = null

    private var loginEnabled = false
    private var accountCreated = false
    private var loading = false
    private var error: Throwable? = null

    var uiState by mutableStateOf(
        UiState(
            loginEnabled = loginEnabled,
            accountCreated = accountCreated,
            loading = loading,
            error = error
        )
    )

    private fun emitState() {
        loginEnabled = !(email.isNullOrBlank() || password.isNullOrBlank()) && !loading

        uiState = UiState(
            loginEnabled = loginEnabled,
            accountCreated = accountCreated,
            loading = loading,
            error = error
        )
    }

    fun onEnterEmail(v: String) {
        email = v
        emitState()
    }

    fun onEnterPassword(v: String) {
        password = v
        emitState()
    }

    fun onResultCaptchaToken(token: String) {
        val tmpEmail = email ?: return
        val tmpPassword = password ?: return

        viewModelScope.launch {
            loading = true
            emitState()

            try {
                val login = coinzixCexApiService.login(tmpEmail, tmpPassword, token)
                val cexType = CexType.Coinzix(login.token, login.data.secret)
                val name = accountFactory.getNextCexAccountName(cexType)
                val account = accountFactory.account(
                    name,
                    AccountType.Cex(cexType),
                    AccountOrigin.Restored,
                    true,
                    false
                )

                accountManager.save(account)
                accountCreated = true
            } catch (err: Throwable) {
                loading = false
                error = err
            }

            emitState()
        }
    }

    data class UiState(
        var loginEnabled: Boolean,
        var accountCreated: Boolean,
        var loading: Boolean,
        var error: Throwable?,
    )
}
