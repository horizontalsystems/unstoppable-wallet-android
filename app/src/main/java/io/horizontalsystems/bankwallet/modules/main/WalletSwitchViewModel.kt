package io.horizontalsystems.bankwallet.modules.main

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Account
import javax.inject.Inject

@HiltViewModel
class WalletSwitchViewModel @Inject constructor(
    private val accountManager: IAccountManager
) : ViewModelUiState<WalletSwitchViewModel.UiState>() {

    private val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    private val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    private val activeWallet: Account?
        get() = accountManager.activeAccount

    override fun createState() = UiState(
        wallets = wallets,
        watchWallets = watchWallets,
        activeWallet = activeWallet
    )

    fun onSelect(account: Account) {
        accountManager.setActiveAccountId(account.id)
    }

    data class UiState(
        val wallets: List<Account>,
        val watchWallets: List<Account>,
        val activeWallet: Account?
    )

}
