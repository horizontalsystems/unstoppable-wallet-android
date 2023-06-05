package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.ZCashConfig

class RestoreViewModel: ViewModel() {

    var accountType: AccountType? = null
        private set

    var accountName: String = ""
        private set

    var manualBackup: Boolean = false
        private set

    var fileBackup: Boolean = false
        private set

    var zCashConfig: ZCashConfig? = null
        private set

    var cancelZCashConfig: Boolean = false

    fun setAccountData(accountType: AccountType, accountName: String, manualBackup: Boolean, fileBackup: Boolean) {
        this.accountType = accountType
        this.accountName = accountName
        this.manualBackup = manualBackup
        this.fileBackup = fileBackup
    }

    fun setZCashConfig(config: ZCashConfig?) {
        zCashConfig = config
    }

}