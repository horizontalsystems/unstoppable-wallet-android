package bitcoin.wallet.modules.fulltransactioninfo

import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.TransactionRecordNew
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import java.util.*

class FullTransactionInfoInteractor(private val adapter: IAdapter?, private val exchangeRateManager: ExchangeRateManager, private val transactionId: String, private var clipboardManager: IClipboardManager) : FullTransactionInfoModule.IInteractor {

    private var transactionRecordViewItem: TransactionRecordViewItem? = null
    var delegate: FullTransactionInfoModule.IInteractorDelegate? = null

    override fun retrieveTransaction() {
        adapter?.let { adapter ->
            adapter.transactionRecordsSubject.subscribe {
                updateTransaction(adapter, transactionId)
            }
            updateTransaction(adapter, transactionId)
        }
    }

    private fun updateTransaction(adapter: IAdapter, transactionId: String) {
        val transaction = adapter.transactionRecords.firstOrNull { it.transactionHash == transactionId }
        transaction?.let {
            val viewItem = getTransactionRecordViewItem(it, adapter)
            transactionRecordViewItem = viewItem
            delegate?.didGetTransactionInfo(viewItem)
        }
    }

    private fun getTransactionRecordViewItem(record: TransactionRecordNew, adapter: IAdapter): TransactionRecordViewItem {
        val rates = exchangeRateManager.exchangeRates
        val convertedValue = rates[adapter.coin.code]?.let { it * record.amount }

        return TransactionRecordViewItem(
                hash = record.transactionHash,
                adapterId = adapter.id,
                amount = CoinValue(adapter.coin, record.amount),
                currencyAmount = convertedValue?.let { CurrencyValue(currency = DollarCurrency(), value = it) },
                fee = CoinValue(coin = adapter.coin, value = record.fee),
                from = record.from.first(),
                to = record.to.first(),
                incoming = record.amount > 0,
                blockHeight = record.blockHeight,
                date = record.timestamp?.let { Date(it) },
                confirmations = TransactionRecordViewItem.getConfirmationsCount(record.blockHeight, adapter.latestBlockHeight)
        )
    }

    override fun getTransactionInfo() {
        transactionRecordViewItem?.let { delegate?.didGetTransactionInfo(it) }
    }

    override fun onCopyFromAddress() {
        transactionRecordViewItem?.from?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }

    override fun onCopyToAddress() {
        transactionRecordViewItem?.to?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }

    override fun showBlockInfo() {
        transactionRecordViewItem?.let { delegate?.showBlockInfo(it) }
    }

    override fun openShareDialog() {
        transactionRecordViewItem?.let { delegate?.openShareDialog(it) }
    }

    override fun onCopyTransactionId() {
        transactionRecordViewItem?.hash?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }
}
