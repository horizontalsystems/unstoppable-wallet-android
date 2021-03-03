package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.core.ICurrencyManager

class TransactionInfoInteractor(
        private var clipboardManager: IClipboardManager,
        private val adapter: ITransactionsAdapter,
        private val xRateManager: IRateManager,
        private val currencyManager: ICurrencyManager,
        private val feeCoinProvider: FeeCoinProvider,
        buildConfigProvider: IBuildConfigProvider
) : TransactionInfoModule.Interactor {
    var delegate: TransactionInfoModule.InteractorDelegate? = null

    override val lastBlockInfo: LastBlockInfo?
        get() = adapter.lastBlockInfo

    override val testMode = buildConfigProvider.testMode

    override fun getRate(coinType: CoinType, timestamp: Long): CurrencyValue? {
        val baseCurrency = currencyManager.baseCurrency

        return xRateManager.historicalRateCached(coinType, baseCurrency.code, timestamp)?.let {
            CurrencyValue(baseCurrency, it)
        }
    }

    override fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

    override fun feeCoin(coin: Coin): Coin? {
        return feeCoinProvider.feeCoinData(coin)?.first
    }

    override fun getRaw(transactionHash: String): String? {
        return adapter.getRawTransaction(transactionHash)
    }
}
