package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig

class RestoreViewModel: ViewModel() {

    var accountType: AccountType? = null
        private set

    var accountName: String = ""
        private set

    var manualBackup: Boolean = false
        private set

    var fileBackup: Boolean = false
        private set

    var birthdayHeightConfig: BirthdayHeightConfig? = null
        private set

    var statPage: StatPage? = null
        private set

    var cancelBirthdayHeightConfig: Boolean = false

    fun setAccountData(accountType: AccountType, accountName: String, manualBackup: Boolean, fileBackup: Boolean, statPage: StatPage) {
        this.accountType = accountType
        this.accountName = accountName
        this.manualBackup = manualBackup
        this.fileBackup = fileBackup
        this.statPage = statPage
    }

    fun setBirthdayHeightConfig(config: BirthdayHeightConfig?) {
        birthdayHeightConfig = config
    }

}