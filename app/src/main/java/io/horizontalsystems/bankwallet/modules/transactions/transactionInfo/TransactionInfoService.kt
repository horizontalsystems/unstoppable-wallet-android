package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.core.EthereumKit

class TransactionInfoService(
    private var clipboardManager: IClipboardManager,
    private val adapter: ITransactionsAdapter,
    private val xRateManager: IRateManager,
    private val currencyManager: ICurrencyManager,
    private val feeCoinProvider: FeeCoinProvider,
    buildConfigProvider: IBuildConfigProvider,
    private val accountSettingManager: AccountSettingManager
) {

    val lastBlockInfo: LastBlockInfo?
        get() = adapter.lastBlockInfo

    val testMode = buildConfigProvider.testMode

    fun getRate(coinType: CoinType, timestamp: Long): CurrencyValue? {
        val baseCurrency = currencyManager.baseCurrency

        return xRateManager.historicalRateCached(coinType, baseCurrency.code, timestamp)?.let {
            CurrencyValue(baseCurrency, it)
        }
    }

    fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

    fun feeCoin(coin: Coin): Coin? {
        return feeCoinProvider.feeCoinData(coin)?.first
    }

    fun getRaw(transactionHash: String): String? {
        return adapter.getRawTransaction(transactionHash)
    }

    fun ethereumNetworkType(account: Account): EthereumKit.NetworkType {
        return accountSettingManager.ethereumNetwork(account).networkType
    }

}
