package io.horizontalsystems.bankwallet.modules.noaccount

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.BitcoinCashCoinTypeManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin

class NoAccountService(
        private val accountManager: IAccountManager,
        private val accountCreator: IAccountCreator,
        private val walletManager: IWalletManager,
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val bitcoinCashCoinTypeManager: BitcoinCashCoinTypeManager
) : NoAccountModule.INoAccountService {

    override fun save(account: Account) {
        accountManager.save(account)
    }

    override fun createAccount(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun createWallet(coin: Coin, account: Account) {
        val wallet = Wallet(coin, account)
        walletManager.save(listOf(wallet))
    }

    override fun resetAddressFormatSettings() {
        derivationSettingsManager.resetStandardSettings()
        bitcoinCashCoinTypeManager.reset()
    }
}
