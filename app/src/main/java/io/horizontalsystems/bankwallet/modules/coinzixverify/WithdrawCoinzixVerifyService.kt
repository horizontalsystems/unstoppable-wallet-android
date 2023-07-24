package cash.p.terminal.modules.coinzixverify

import cash.p.terminal.core.providers.CoinzixCexProvider

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
