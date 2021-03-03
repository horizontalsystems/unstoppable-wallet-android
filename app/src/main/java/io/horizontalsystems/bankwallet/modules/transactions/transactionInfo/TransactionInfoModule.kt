package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import java.util.*

object TransactionInfoModule {
    interface View {
        fun showCopied()
        fun share(value: String)
        fun showTitle(titleViewItem: TitleViewItem)
        fun showDetails(items: List<TransactionDetailViewItem>)
        fun setExplorerButton(explorerName: String, enabled: Boolean)
    }

    interface ViewDelegate {
        fun viewDidLoad()
        fun onShare()
        fun openExplorer()
        fun onClickLockInfo()
        fun onClickDoubleSpendInfo()
        fun onClickRecipientHash()
        fun onClickTo()
        fun onClickFrom()
        fun onClickTransactionId()
        fun onRawTransaction()
        fun onClickStatusInfo()
    }

    interface Interactor {
        val lastBlockInfo: LastBlockInfo?
        val testMode: Boolean

        fun copyToClipboard(value: String)
        fun getRate(coinType: CoinType, timestamp: Long): CurrencyValue?
        fun feeCoin(coin: Coin): Coin?
        fun getRaw(transactionHash: String): String?
    }

    interface InteractorDelegate

    interface Router {
        fun openLockInfo(lockDate: Date)
        fun openDoubleSpendInfo(transactionHash: String, conflictingTxHash: String)
        fun openStatusInfo()
        fun openUrl(url: String)
    }

    fun init(view: TransactionInfoViewModel, router: Router, transactionRecord: TransactionRecord, wallet: Wallet) {
        val adapter = App.adapterManager.getTransactionsAdapterForWallet(wallet)!!
        val interactor = TransactionInfoInteractor(TextHelper, adapter, App.xRateManager, App.currencyManager, App.feeCoinProvider, App.buildConfigProvider)
        val presenter = TransactionInfoPresenter(interactor, router, transactionRecord, wallet, TransactionInfoAddressMapper)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    data class TitleViewItem(val date: Date?, val primaryAmountInfo: SendModule.AmountInfo, val secondaryAmountInfo: SendModule.AmountInfo?, val type: TransactionType, val lockState: TransactionLockState?)

    data class ExplorerData (val title: String, val url: String?)
}
