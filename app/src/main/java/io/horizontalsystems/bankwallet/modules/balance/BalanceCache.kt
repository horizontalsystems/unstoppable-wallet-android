package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Wallet
import java.math.BigDecimal

class BalanceCache(private val walletStorage: IWalletStorage) {

    fun setCache(wallet: Wallet, balance: BigDecimal, balanceLocked: BigDecimal) {
        walletStorage.save(wallet.copy(balance = balance, balanceLocked = balanceLocked))
    }

    fun getCache(wallet: Wallet): Pair<BigDecimal, BigDecimal> {
        return Pair(wallet.balance, wallet.balanceLocked)
    }

}
