package io.horizontalsystems.bankwallet.modules.coinzixverify

interface ICoinzixVerifyService {
    suspend fun verify(emailCode: String?, googleCode: String?)
    suspend fun resendPin()
}
