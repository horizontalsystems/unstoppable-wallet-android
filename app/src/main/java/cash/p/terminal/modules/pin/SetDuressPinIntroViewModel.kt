package cash.p.terminal.modules.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.wallet.IAccountManager
import io.horizontalsystems.core.ISystemInfoManager

class SetDuressPinIntroViewModel(
    systemInfoManager: ISystemInfoManager,
    accountManager: cash.p.terminal.wallet.IAccountManager,
) : ViewModel() {
    val biometricAuthSupported = systemInfoManager.biometricAuthSupported
    val shouldShowSelectAccounts = accountManager.accounts.isNotEmpty()

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SetDuressPinIntroViewModel(App.systemInfoManager, App.accountManager) as T
        }
    }

}
