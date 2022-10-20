package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable

class WatchAddressBlockchainManager(
    accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val walletActivator: WalletActivator,
) {

    private val disposables = CompositeDisposable()

    init {
        accountManager.activeAccountObservable
            .subscribeIO {
                enableDisabledBlockchains(it.orElse(null))
            }
            .let {
                disposables.add(it)
            }
        enableDisabledBlockchains(accountManager.activeAccount)
    }

    private fun enableDisabledBlockchains(account: Account?) {
        account ?: return

        if (!account.isWatchAccount) {
            return
        }

        if (account.type is AccountType.Address) {
            val wallets = walletManager.getWallets(account)
            val enabledBlockchains = wallets.map { it.token.blockchain }
            val disabledBlockchains = evmBlockchainManager.allBlockchains
                .filter { !enabledBlockchains.contains(it) }

            if (disabledBlockchains.isEmpty()) {
                return
            }

            try {
                val tokenQueries = disabledBlockchains.map { TokenQuery(it.type, TokenType.Native) }
                walletActivator.activateWallets(account, tokenQueries)
            } catch (e: Exception) {

            }
        }

    }
}
