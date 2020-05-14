package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router,
        private val transactionHash: String,
        private val wallet: Wallet,
        private val transactionViewItemFactory: TransactionViewItemFactory
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null

    private val transactionRecord: TransactionRecord

    init {
        transactionRecord = interactor.getTransactionRecord(wallet, transactionHash) ?: throw IllegalStateException("Transaction Not Found")
    }


    // IViewDelegate methods

    override fun viewDidLoad() {
        val threshold = interactor.getThreshold(wallet)
        val lastBlockInfo = interactor.getLastBlockInfo(wallet)
        val rate = interactor.getRate(wallet.coin.code, transactionRecord.timestamp)

        val item = transactionViewItemFactory.item(wallet, transactionRecord, lastBlockInfo, threshold, rate)
        view?.showTransaction(item)
    }

    override fun onShare() {
        view?.share(transactionRecord.transactionHash)
    }

    override fun openFullInfo() {
        router.openFullInfo(transactionRecord.transactionHash, wallet)
    }

    override fun onClickLockInfo() {
        transactionRecord.lockInfo?.lockedUntil?.let {
            router.openLockInfo(it)
        }
    }

    override fun onClickDoubleSpendInfo() {
        transactionRecord.conflictingTxHash?.let { conflictingTxHash ->
            router.openDoubleSpendInfo(transactionRecord.transactionHash, conflictingTxHash)
        }
    }

    override fun onClickRecipientHash() {
        onCopy(transactionRecord.lockInfo?.originalAddress)
    }

    override fun onClickTo() {
        onCopy(transactionRecord.to)
    }

    override fun onClickFrom() {
        onCopy(transactionRecord.from)
    }

    override fun onClickTransactionId() {
        onCopy(transactionRecord.transactionHash)
    }

    private fun onCopy(value: String?) {
        value?.let {
            interactor.onCopy(value)
            view?.showCopied()
        }
    }
}
