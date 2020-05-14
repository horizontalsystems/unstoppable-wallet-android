package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import java.util.*

object TransactionInfoModule {
    interface View {
        fun showCopied()
        fun share(value: String)
        fun showTransaction(item: TransactionViewItem)
        fun showTitle(titleViewItem: TitleViewItem)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onShare()
        fun openFullInfo()
        fun onClickLockInfo()
        fun onClickDoubleSpendInfo()
        fun onClickRecipientHash()
        fun onClickTo()
        fun onClickFrom()
        fun onClickTransactionId()
    }

    interface Interactor {
        fun onCopy(value: String)
        fun getTransactionRecord(wallet: Wallet, transactionHash: String): TransactionRecord?
        fun getLastBlockInfo(wallet: Wallet): LastBlockInfo?
        fun getThreshold(wallet: Wallet): Int
        fun getRate(code: String, timestamp: Long): CurrencyValue?
    }

    interface InteractorDelegate

    interface Router {
        fun openFullInfo(transactionHash: String, wallet: Wallet)
        fun openLockInfo(lockDate: Date)
        fun openDoubleSpendInfo(transactionHash: String, conflictingTxHash: String)
    }

    fun init(view: TransactionInfoViewModel, router: Router, transactionHash: String, wallet: Wallet) {
        val interactor = TransactionInfoInteractor(TextHelper, App.adapterManager, App.xRateManager, App.currencyManager)
        val presenter = TransactionInfoPresenter(interactor, router, transactionHash, wallet, TransactionViewItemFactory(App.feeCoinProvider))

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, transactionHash: String, wallet: Wallet) {
        (activity as? MainActivity)?.openTransactionInfo(transactionHash, wallet)
    }

    data class TitleViewItem(val date: Date?, val primaryAmountInfo: SendModule.AmountInfo, val secondaryAmountInfo: SendModule.AmountInfo?, val type: TransactionType, val locked: Boolean?)
}
