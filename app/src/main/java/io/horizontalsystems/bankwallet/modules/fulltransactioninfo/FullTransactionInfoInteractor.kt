package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FullTransactionInfoInteractor(private val transactionProvider: FullTransactionInfoModule.FullProvider, private var clipboardManager: IClipboardManager)
    : FullTransactionInfoModule.Interactor, FullTransactionInfoModule.ProviderDelegate {

    val disposables = CompositeDisposable()
    var delegate: FullTransactionInfoModule.InteractorDelegate? = null

    //
    // Interactor implementations
    //
    override fun url(hash: String): String {
        return transactionProvider.url(hash)
    }

    override fun retrieveTransactionInfo(transactionHash: String) {
        disposables.clear()
        disposables.add(transactionProvider.retrieveTransactionInfo(transactionHash)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.onReceiveTransactionInfo(it)
                }, {
                    delegate?.onError(transactionProvider.providerName)
                })
        )
    }

    override fun copyToClipboard(value: String) {
        clipboardManager.copyText(value)
    }

    //
    // ProviderDelegate implementations
    //
    override fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord) {
        delegate?.onReceiveTransactionInfo(transactionRecord)
    }
}
