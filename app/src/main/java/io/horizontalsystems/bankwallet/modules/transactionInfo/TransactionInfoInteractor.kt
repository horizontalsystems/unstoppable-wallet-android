package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ITransactionRecordStorage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class TransactionInfoInteractor(
        private val transactionRepository: ITransactionRecordStorage,
        private var clipboardManager: IClipboardManager) : TransactionInfoModule.IInteractor {

    var delegate: TransactionInfoModule.IInteractorDelegate? = null

    override fun getTransaction(transactionHash: String) {
        val disposable = transactionRepository.record(transactionHash)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { record ->
                    delegate?.didGetTransaction(record)
                }
    }

    override fun onCopy(value: String) {
        clipboardManager.copyText(value)
    }

    override fun showFullInfo() {
//        delegate?.showFullInfo(transactionRecordViewItem)
    }

}
