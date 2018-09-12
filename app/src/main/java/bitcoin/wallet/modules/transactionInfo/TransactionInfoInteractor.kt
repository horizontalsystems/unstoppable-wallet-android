package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoInteractor(private val transactionRecordViewItem: TransactionRecordViewItem, private var clipboardManager: IClipboardManager) : TransactionInfoModule.IInteractor {

    var delegate: TransactionInfoModule.IInteractorDelegate? = null

    override fun getTransactionInfo() {
        delegate?.didGetTransactionInfo(transactionRecordViewItem)
    }

    override fun onCopyFromAddress() {
        transactionRecordViewItem.from?.let {
            clipboardManager.copyText(it)
            delegate?.didCopyToClipboard()
        }
    }

    override fun onCopyId() {
        clipboardManager.copyText(transactionRecordViewItem.hash)
        delegate?.didCopyToClipboard()
    }

    override fun showFullInfo() {
        delegate?.showFullInfo(transactionRecordViewItem)
    }

}
