package io.horizontalsystems.bankwallet.modules.send.zcash

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ISendZCashAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.Single
import java.math.BigDecimal

class SendZcashInteractor(
        private val adapter: ISendZCashAdapter
) : SendModule.ISendZcashInteractor {

    override val availableBalance: BigDecimal
        get() = adapter.availableBalance

    override val fee: BigDecimal
        get() = adapter.fee

    override fun validate(address: String) {
        adapter.validate(address)
    }

    override fun send(amount: BigDecimal, address: String, memo: String?, logger: AppLogger): Single<Unit> {
        return adapter.send(amount, address, memo ?: "", logger)
    }

}
