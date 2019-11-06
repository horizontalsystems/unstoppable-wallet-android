package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal


class SendAmountInteractor(
        private val baseCurrency: Currency,
        private val rateManager: IRateManager,
        private val localStorage: ILocalStorage,
        private val coin: Coin
): SendAmountModule.IInteractor {

    override var defaultInputType: SendModule.InputType
        get() = localStorage.sendInputType ?: SendModule.InputType.COIN
        set(value) { localStorage.sendInputType = value }

    override fun getRate(): BigDecimal? {
        return rateManager.getLatestRate(coin.code, baseCurrency.code)
    }
}
