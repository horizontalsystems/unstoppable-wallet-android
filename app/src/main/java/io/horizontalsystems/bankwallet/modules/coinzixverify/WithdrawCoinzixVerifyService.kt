package io.horizontalsystems.bankwallet.modules.coinzixverify

import io.horizontalsystems.bankwallet.core.providers.CoinzixCexProvider

class WithdrawCoinzixVerifyService(
    private val provider: CoinzixCexProvider,
    private val withdrawId: String
) : ICoinzixVerifyService {

    override suspend fun verify(emailCode: String?, googleCode: String?) {
        provider.confirmWithdraw(withdrawId, emailCode, googleCode)
    }

    override suspend fun resendPin() {
        provider.sendWithdrawPin(withdrawId)
    }
}
