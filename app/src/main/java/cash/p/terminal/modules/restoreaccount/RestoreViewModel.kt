package cash.p.terminal.modules.restoreaccount

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.enablecoin.restoresettings.ZCashConfig

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

    var statPage: StatPage? = null
        private set

    var cancelZCashConfig: Boolean = false

    fun setAccountData(accountType: AccountType, accountName: String, manualBackup: Boolean, fileBackup: Boolean, statPage: StatPage) {
        this.accountType = accountType
        this.accountName = accountName
        this.manualBackup = manualBackup
        this.fileBackup = fileBackup
        this.statPage = statPage
    }

    fun setZCashConfig(config: ZCashConfig?) {
        zCashConfig = config
    }

}