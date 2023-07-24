package cash.p.terminal.modules.coinzixverify

interface ICoinzixVerifyService {
    suspend fun verify(emailCode: String?, googleCode: String?)
    suspend fun resendPin()
}
