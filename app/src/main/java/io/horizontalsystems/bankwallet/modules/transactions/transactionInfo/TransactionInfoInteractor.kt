package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.ICurrencyManager

class TransactionInfoInteractor(
        private var clipboardManager: IClipboardManager,
        private val adapterManager: IAdapterManager,
        private val xRateManager: IRateManager,
        private val currencyManager: ICurrencyManager
) : TransactionInfoModule.Interactor {
    var delegate: TransactionInfoModule.InteractorDelegate? = null

    override fun onCopy(value: String) {
        clipboardManager.copyText(value)
    }

    override fun getTransactionRecord(wallet: Wallet, transactionHash: String): TransactionRecord? {
        return adapterManager.getTransactionsAdapterForWallet(wallet)?.getTransaction(transactionHash)
    }

    override fun getLastBlockInfo(wallet: Wallet): LastBlockInfo? {
        return adapterManager.getTransactionsAdapterForWallet(wallet)?.lastBlockInfo
    }

    override fun getThreshold(wallet: Wallet): Int {
        return adapterManager.getTransactionsAdapterForWallet(wallet)?.confirmationsThreshold ?: 0
    }

    override fun getRate(code: String, timestamp: Long): CurrencyValue? {
        val baseCurrency = currencyManager.baseCurrency
        return try {
            val rateValue = xRateManager.historicalRate(code, baseCurrency.code, timestamp).blockingGet()
            CurrencyValue(baseCurrency, rateValue)
        } catch (e: Exception) {
            null
        }
    }
}
