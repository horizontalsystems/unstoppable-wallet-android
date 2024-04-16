package io.horizontalsystems.bankwallet.modules.walletconnect

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account

class WCManager(
    private val accountManager: IAccountManager,
) {
    sealed class SupportState {
        object Supported : SupportState()
        object NotSupportedDueToNoActiveAccount : SupportState()
        class NotSupportedDueToNonBackedUpAccount(val account: Account) : SupportState()
        class NotSupported(val accountTypeDescription: String) : SupportState()
    }

    fun getWalletConnectSupportState(): SupportState {
        val tmpAccount = accountManager.activeAccount
        return when {
            tmpAccount == null -> SupportState.NotSupportedDueToNoActiveAccount
            !tmpAccount.isBackedUp && !tmpAccount.isFileBackedUp -> SupportState.NotSupportedDueToNonBackedUpAccount(
                tmpAccount
            )
            tmpAccount.type.supportsWalletConnect -> SupportState.Supported
            else -> SupportState.NotSupported(tmpAccount.type.description)
        }
    }

}
