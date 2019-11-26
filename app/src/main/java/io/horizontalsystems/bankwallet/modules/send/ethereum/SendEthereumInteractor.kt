package io.horizontalsystems.bankwallet.modules.send.ethereum

import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.Single
import java.math.BigDecimal

class SendEthereumInteractor(private val adapter: ISendEthereumAdapter) : SendModule.ISendEthereumInteractor {
    override val ethereumBalance: BigDecimal
        get() = adapter.ethereumBalance

    override val minimumRequiredBalance: BigDecimal
        get() = adapter.minimumRequiredBalance

    override val minimumAmount: BigDecimal
        get() = adapter.minimumSendAmount

    override fun availableBalance(gasPrice: Long): BigDecimal {
        return adapter.availableBalance(gasPrice)
    }

    override fun validate(address: String) {
        adapter.validate(address)
    }

    override fun fee(gasPrice: Long): BigDecimal {
        return adapter.fee(gasPrice)
    }

    override fun send(amount: BigDecimal, address: String, gasPrice: Long, gasLimit: Long): Single<Unit> {
        return adapter.send(amount, address, gasPrice, gasLimit )
    }

}
