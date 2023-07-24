package io.horizontalsystems.bankwallet.modules.coinzixverify

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.bankwallet.modules.balance.cex.CoinzixCexApiService

class LoginCoinzixVerifyService(
    private val token: String,
    private val secret: String,
    private val api: CoinzixCexApiService,
    private val accountManager: IAccountManager,
    private val accountFactory: IAccountFactory
) : ICoinzixVerifyService {
    override suspend fun verify(emailCode: String?, googleCode: String?) {
        val response = api.validateCode(token, emailCode ?: googleCode ?: "")

        check(response.status) { response.errors?.joinToString { it } ?: "Unknown error" }

        val cexType = CexType.Coinzix(token, secret)
        val name = accountFactory.getNextCexAccountName(cexType)
        val account = accountFactory.account(
            name,
            AccountType.Cex(cexType),
            AccountOrigin.Restored,
            true,
            false
        )
        accountManager.save(account)
    }

    override suspend fun resendPin() {
        val response = api.resendLoginPin(token)

        check(response.status) { response.errors?.joinToString { it } ?: "Unknown error" }
    }
}
