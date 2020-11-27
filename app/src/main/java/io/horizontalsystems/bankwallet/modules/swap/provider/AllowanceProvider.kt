package io.horizontalsystems.bankwallet.modules.swap.provider

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.adapters.Erc20Adapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.reactivex.Single
import java.math.BigDecimal

class AllowanceProvider(
        private val adapterManager: IAdapterManager
) {
    fun getAllowance(coin: Coin, spenderAddress: Address): Single<BigDecimal> {
        val adapter = adapterManager.getAdapterForCoin(coin) as? Erc20Adapter
                ?: throw UnsupportedAccountException()
        return adapter.allowance(spenderAddress, DefaultBlockParameter.Latest)
    }
}
