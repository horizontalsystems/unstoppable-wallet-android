package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.bankwallet.modules.balance.cex.CoinzixCexApiService
import kotlinx.coroutines.launch

class EnterCexDataCoinzixViewModel : ViewModel() {
    private val accountManager = App.accountManager
    private val accountFactory = App.accountFactory
    private val coinzixCexApiService = CoinzixCexApiService()

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
        }
    }
}
