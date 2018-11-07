package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem

class TransactionInfoInteractor(private val transactionRecordViewItem: TransactionRecordViewItem, private var clipboardManager: IClipboardManager) : TransactionInfoModule.IInteractor {

    var delegate: TransactionInfoModule.IInteractorDelegate? = null

    override fun getTransactionInfo() {
        delegate?.didGetTransactionInfo(transactionRecordViewItem)
    }

    override fun onCopyAddress() {
        val address = if (transactionRecordViewItem.incoming) transactionRecordViewItem.from else transactionRecordViewItem.to
        address?.let {
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
