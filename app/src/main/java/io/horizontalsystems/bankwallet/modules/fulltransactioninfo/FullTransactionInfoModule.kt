package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.support.v4.app.FragmentActivity
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.entities.FullTransactionSection
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable

object FullTransactionInfoModule {
    interface View {
        fun show()
        fun showLoading()
        fun hideLoading()
        fun reload()
    }

    interface ViewDelegate {
        fun viewDidLoad()

        val resource: String
        val sectionCount: Int
        fun getSection(row: Int): FullTransactionSection?
    }

    interface Interactor {
        fun retrieveTransactionInfo(transactionHash: String)
    }

    interface InteractorDelegate {
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
    }

    interface Router

    interface Provider {
        val resource: String
        fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord>
    }

    interface ProviderFactory {
        fun providerFor(coinCode: CoinCode): Provider
    }

    interface ProviderDelegate {
        fun onReceiveTransactionInfo(transactionRecord: FullTransactionRecord)
    }

    interface State {
        val transactionHash: String
        var transactionRecord: FullTransactionRecord?
    }

    fun init(view: FullTransactionInfoViewModel, router: Router, transactionProvider: Provider, transactionHash: String) {
        val interactor = FullTransactionInfoInteractor(transactionProvider)
        val dataSource = FullTransactionInfoState(transactionHash)
        val presenter = FullTransactionInfoPresenter(interactor, router, dataSource)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, transactionHash: String, coinCode: CoinCode) {
        FullTransactionInfoActivity.start(activity, transactionHash, coinCode)
    }
}
