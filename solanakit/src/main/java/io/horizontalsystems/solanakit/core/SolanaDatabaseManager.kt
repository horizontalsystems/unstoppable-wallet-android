package io.horizontalsystems.solanakit.core

import android.content.Context
import io.horizontalsystems.solanakit.database.main.MainDatabase
import io.horizontalsystems.solanakit.database.transaction.TransactionDatabase

internal object SolanaDatabaseManager {

    fun getMainDatabase(context: Context, walletId: String): MainDatabase {
        return MainDatabase.getInstance(context, getDbNameMain(walletId))
    }

    fun getTransactionDatabase(context: Context, walletId: String): TransactionDatabase {
        return TransactionDatabase.getInstance(context, getDbNameTransactions(walletId))
    }

    fun clear(context: Context, walletId: String) {
        synchronized(this) {
            context.deleteDatabase(getDbNameMain(walletId))
            context.deleteDatabase(getDbNameTransactions(walletId))
        }
    }

    private fun getDbNameMain(walletId: String): String {
        return getDbName(walletId, "main")
    }

    private fun getDbNameTransactions(walletId: String): String {
        return getDbName(walletId, "txs")
    }

    private fun getDbName(walletId: String, suffix: String): String {
        return "Solana-$walletId-$suffix"
    }

}
