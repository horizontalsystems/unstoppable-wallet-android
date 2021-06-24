package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Wallet

class BalanceCache(private val walletStorage: IWalletStorage) {

    fun setCache(wallet: Wallet, balanceData: BalanceData) {
        setCache(mapOf(wallet to balanceData))
    }

    fun getCache(wallet: Wallet) = BalanceData(wallet.balance, wallet.balanceLocked)

    fun setCache(balancesData: Map<Wallet, BalanceData>) {
        walletStorage.save(balancesData.map { (wallet, balanceData) ->
            wallet.copy(balance = balanceData.available, balanceLocked = balanceData.locked)
        })
    }

}
