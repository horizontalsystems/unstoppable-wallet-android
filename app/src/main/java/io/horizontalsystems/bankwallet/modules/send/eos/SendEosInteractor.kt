package io.horizontalsystems.bankwallet.modules.send.eos

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ISendEosAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.Single
import java.math.BigDecimal

class SendEosInteractor(private val adapter: ISendEosAdapter) : SendModule.ISendEosInteractor {

    override val availableBalance: BigDecimal
        get() = adapter.availableBalance

    override fun validate(account: String) {
        adapter.validate(account)
    }

    override fun send(amount: BigDecimal, account: String, memo: String?, logger: AppLogger): Single<Unit> {
        return adapter.send(amount, account, memo, logger)
    }

}
