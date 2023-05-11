package cash.p.terminal.modules.restoreaccount

import androidx.lifecycle.ViewModel
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.enablecoin.restoresettings.ZCashConfig

class RestoreViewModel: ViewModel() {

    var accountType: AccountType? = null
        private set

    var accountName: String = ""
        private set

    var zCashConfig: ZCashConfig? = null
        private set

    var cancelZCashConfig: Boolean = false

    fun setAccountData(accountType: AccountType, accountName: String) {
        this.accountType = accountType
        this.accountName = accountName
    }

    fun setZCashConfig(config: ZCashConfig?) {
        zCashConfig = config
    }

}