package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FullTransactionInfoInteractor(private val transactionProvider: FullTransactionInfoModule.Provider)
    : FullTransactionInfoModule.Interactor, FullTransactionInfoModule.ProviderDelegate {

    var delegate: FullTransactionInfoModule.InteractorDelegate? = null

    //
    // Interactor implementations
    //
    override fun retrieveTransactionInfo(transactionHash: String) {
        val a = transactionProvider.retrieveTransactionInfo(transactionHash)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.onReceiveTransactionInfo(it)
                }

    }

    //
    // ProviderDelegate implementations
    //
    override fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord) {
        delegate?.onReceiveTransactionInfo(transactionRecord)
    }
}
