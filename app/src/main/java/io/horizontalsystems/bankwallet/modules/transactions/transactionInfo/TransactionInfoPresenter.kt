package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.Wallet
import java.util.*

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null

    // IViewDelegate methods

    override fun onCopy(value: String) {
        interactor.onCopy(value)
        view?.showCopied()
    }

    override fun openFullInfo(transactionHash: String, wallet: Wallet) {
        router.openFullInfo(transactionHash, wallet)
    }

    override fun onClickLockInfo(lockDate: Date) {
        router.openLockInfo(lockDate)
    }

    override fun onClickDoubleSpendInfo(transactionHash: String, conflictingTxHash: String) {
        router.openDoubleSpendInfo(transactionHash, conflictingTxHash)
    }

}
