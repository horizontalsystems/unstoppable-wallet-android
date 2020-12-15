package io.horizontalsystems.bankwallet.modules.noaccount

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.entities.*

class NoAccountService(
        private val accountManager: IAccountManager,
        private val accountCreator: IAccountCreator,
        private val derivationSettingsManager: IDerivationSettingsManager
) : NoAccountModule.INoAccountService {

    override fun save(account: Account) {
        accountManager.save(account)
    }

    override fun createAccount(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun derivationSetting(coinType: CoinType): DerivationSetting? {
        return derivationSettingsManager.setting(coinType)
    }

    override fun resetDerivationSettings() {
        derivationSettingsManager.reset()
    }
}
