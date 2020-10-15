package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.core.EthereumKit

class WalletConnectSendEthereumTransactionRequestService(
        private val ethereumKit: EthereumKit,
        private val appConfigProvider: IAppConfigProvider,
        private val currencyManager: ICurrencyManager,
        private val xRateManager: IRateManager
) {

    val ethereumCoin: Coin = appConfigProvider.ethereumCoin

    val ethereumRate: CurrencyValue?
        get() {
            val baseCurrency = currencyManager.baseCurrency

            return xRateManager.marketInfo(ethereumCoin.code, baseCurrency.code)?.let {
                CurrencyValue(baseCurrency, it.rate)
            }
        }
}
