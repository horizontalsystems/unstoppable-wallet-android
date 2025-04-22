package io.horizontalsystems.stellarkit

import io.horizontalsystems.stellarkit.room.AssetBalance
import io.horizontalsystems.stellarkit.room.AssetNativeBalance
import io.horizontalsystems.stellarkit.room.BalanceDao
import org.stellar.sdk.Server

class BalancesManager(
    private val server: Server,
    private val balanceDao: BalanceDao,
    private val address: String
) {
    fun sync() {
        val accounts = server.accounts()
        val account = accounts.account(address)
        account.balances.forEach { balance ->
            if (balance.assetType == "native") {
                balanceDao.insertNative(
                    AssetNativeBalance(balance = balance.balance.toBigDecimal())
                )
            } else {
                balanceDao.insert(
                    AssetBalance(
                        type = balance.assetType,
                        code = balance.assetCode,
                        issuer = balance.assetIssuer,
                        balance = balance.balance.toBigDecimal(),
                    )
                )
            }

        }
    }
}
