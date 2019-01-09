package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FullTransactionInfoInteractor(private val transactionProvider: FullTransactionInfoModule.Provider, private var clipboardManager: IClipboardManager)
    : FullTransactionInfoModule.Interactor, FullTransactionInfoModule.ProviderDelegate {

    val disposables = CompositeDisposable()
    var delegate: FullTransactionInfoModule.InteractorDelegate? = null

    //
    // Interactor implementations
    //
    override fun retrieveTransactionInfo(transactionHash: String) {
        disposables.clear()
        disposables.add(transactionProvider.retrieveTransactionInfo(transactionHash)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.onReceiveTransactionInfo(it)
                }, {
                    delegate?.onError()
                })
        )
    }

    override fun retryLoadInfo() {
        delegate?.retryLoadInfo()
    }

    override fun onTapItem(item: FullTransactionItem) {
        if (item.clickable) {
            if (item.url != null) {
                delegate?.onOpenUrl(item.url)
            } else if (item.value != null) {
                clipboardManager.copyText(item.value)
                delegate?.onCopied()
            }
        }
    }

    //
    // ProviderDelegate implementations
    //
    override fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord) {
        delegate?.onReceiveTransactionInfo(transactionRecord)
    }
}
