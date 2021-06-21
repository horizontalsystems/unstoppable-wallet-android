package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Wallet

class BalanceCache(private val walletStorage: IWalletStorage) {

    fun setCache(wallet: Wallet, balanceData: BalanceData) {
        walletStorage.save(wallet.copy(balance = balanceData.available, balanceLocked = balanceData.locked))
    }

    fun getCache(wallet: Wallet) = BalanceData(wallet.balance, wallet.balanceLocked)

}
