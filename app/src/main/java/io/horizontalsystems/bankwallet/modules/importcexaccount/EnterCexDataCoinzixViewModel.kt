package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.balance.cex.CoinzixCexApiService
import io.horizontalsystems.bankwallet.modules.coinzixverify.CoinzixVerificationMode
import io.horizontalsystems.bankwallet.modules.coinzixverify.TwoFactorType
import io.horizontalsystems.core.helpers.DateHelper
import java.util.Date

class EnterCexDataCoinzixViewModel : ViewModel() {
    private val coinzixCexApiService = CoinzixCexApiService()

    private var email: String? = null
    private var password: String? = null

    var loginEnabled by mutableStateOf(false)
        private set

    private fun emitState() {
        loginEnabled = !email.isNullOrBlank() && !password.isNullOrBlank()
    }

    fun onEnterEmail(v: String) {
        email = v
        emitState()
    }

    fun onEnterPassword(v: String) {
        password = v
        emitState()
    }

    suspend fun login(): CoinzixVerificationMode.Login {
        val tmpEmail = email ?: throw IllegalStateException("Email is null")
        val tmpPassword = password ?: throw IllegalStateException("Password is null")

        val login = coinzixCexApiService.login(tmpEmail, tmpPassword)
        val loginData = login.loginData()

        if (login.status) {
            val twoFactorType = TwoFactorType.fromCode(loginData?.required)
            if (login.token != null && loginData?.secret != null && twoFactorType != null) {
                return CoinzixVerificationMode.Login(login.token, loginData.secret, listOf(twoFactorType))
            } else {
                throw Exception("Invalid login data returned")
            }
        } else {
            if (loginData?.left_attempt != null) {
                throw Exception("Invalid login credentials. Attempts left: ${loginData.left_attempt}")
            } else if (loginData?.time_expire != null) {
                val unlockDate = Date(loginData.time_expire.toLong() * 1000)
                val formattedDate = DateHelper.formatDate(unlockDate, "MMM d, yyyy, HH:mm")

                throw Exception("Too many invalid login attempts were made. Login is locked until $formattedDate")
            }

            throw Exception(login.errors?.joinToString("\n") ?: "Unknown error")
        }
    }

}
