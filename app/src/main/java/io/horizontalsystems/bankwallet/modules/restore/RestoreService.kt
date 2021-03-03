package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin

class RestoreService(
        override var predefinedAccountType: PredefinedAccountType?,
        private val walletManager: IWalletManager,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager
): RestoreModule.IRestoreService, Clearable {

    override var accountType: AccountType? = null

    override fun restoreAccount(coins: List<Coin>) {

        val accountType = accountType ?: throw RestoreError.NoAccountType

        val account = accountCreator.restoredAccount(accountType)
        accountManager.save(account)

        if (coins.isEmpty()){
            return
        }

        val wallets = coins.map {
                Wallet(it, account)
        }

        walletManager.save(wallets)
    }


    override fun clear() {

    }

    sealed class RestoreError: Exception() {
        object NoAccountType: RestoreError()
    }
}
