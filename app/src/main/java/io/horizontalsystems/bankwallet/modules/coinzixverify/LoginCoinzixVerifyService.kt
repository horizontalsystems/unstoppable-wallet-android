package cash.p.terminal.modules.coinzixverify

import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.CexType
import cash.p.terminal.modules.balance.cex.CoinzixCexApiService

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
