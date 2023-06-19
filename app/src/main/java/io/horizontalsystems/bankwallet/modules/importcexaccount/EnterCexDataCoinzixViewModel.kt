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
import kotlinx.coroutines.launch

class EnterCexDataCoinzixViewModel : ViewModel() {
    private val accountManager = App.accountManager
    private val accountFactory = App.accountFactory
    private val coinzixLoginService = CoinzixLoginService()

    private var email: String? = null
    private var password: String? = null

    var loginEnabled by mutableStateOf(false)
        private set
    var accountCreated by mutableStateOf(false)
        private set

    private fun emitState() {
        loginEnabled = !(email.isNullOrBlank() || password.isNullOrBlank())
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
            val login = coinzixLoginService.login(tmpEmail, tmpPassword, token)
            val account = accountFactory.account(
                "Coinzix Wallet",
                AccountType.Cex(CexType.Coinzix(login.token, login.data.secret)),
                AccountOrigin.Restored,
                true,
                false
            )

            accountManager.save(account)
            accountCreated = true
        }
    }
}
