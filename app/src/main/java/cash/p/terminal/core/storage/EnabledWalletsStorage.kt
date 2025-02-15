package cash.p.terminal.core.storage

import android.util.Log
import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.entities.EnabledWallet

class EnabledWalletsStorage(private val appDatabase: AppDatabase) : IEnabledWalletStorage {

    override val enabledWallets: List<EnabledWallet>
        get() = appDatabase.walletsDao().enabledCoins()

    override fun enabledWallets(accountId: String): List<EnabledWallet> {
        return appDatabase.walletsDao().enabledCoins(accountId)
    }

    override fun save(enabledWallets: List<EnabledWallet>) {
        appDatabase.walletsDao().insertWallets(enabledWallets)
    }

    override fun deleteAll() {
        appDatabase.walletsDao().deleteAll()
    }

    override fun delete(enabledWallets: List<EnabledWallet>) {
        val tokenQueryIds = enabledWallets.map { it.tokenQueryId.lowercase() }
        val accountIds = enabledWallets.map { it.accountId.lowercase() }
        Log.d("EnabledWalletsStorage", "Deleting tokenQueryIds:$tokenQueryIds, accountIds:$accountIds wallets")
        appDatabase.walletsDao().enabledCoins().forEach { wallet ->
            Log.d("EnabledWalletsStorage", "Existing wallet: tokenQueryId:${wallet.tokenQueryId}, accountId:${wallet.accountId}")
        }
        appDatabase.walletsDao().deleteWallets(
            tokenQueryIds = tokenQueryIds,
            accountIds = accountIds
        )
    }
}
