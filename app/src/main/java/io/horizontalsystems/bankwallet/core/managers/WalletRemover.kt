package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.reactivex.schedulers.Schedulers

class WalletRemover(accountManager: IAccountManager, private val walletManager: IWalletManager) {

    val disposable = accountManager.deleteAccountObservable
            .observeOn(Schedulers.io())
            .subscribe {
                handleDelete(it)
            }

    private fun handleDelete(accountId: String) {
        val remainingWallets = walletManager.wallets.filter { it.account.id != accountId }
        walletManager.enable(remainingWallets)
    }
}
