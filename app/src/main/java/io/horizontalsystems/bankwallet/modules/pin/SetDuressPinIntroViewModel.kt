package io.horizontalsystems.bankwallet.modules.pin

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.core.ISystemInfoManager
import javax.inject.Inject

@HiltViewModel
class SetDuressPinIntroViewModel @Inject constructor(
    systemInfoManager: ISystemInfoManager,
    accountManager: IAccountManager,
) : ViewModel() {
    val biometricAuthSupported = systemInfoManager.biometricAuthSupported
    val shouldShowSelectAccounts = accountManager.accounts.isNotEmpty()
}
