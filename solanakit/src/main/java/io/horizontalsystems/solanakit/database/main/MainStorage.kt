package io.horizontalsystems.solanakit.database.main

import io.horizontalsystems.solanakit.models.BalanceEntity
import io.horizontalsystems.solanakit.models.InitialSyncEntity
import io.horizontalsystems.solanakit.models.LastBlockHeightEntity

class MainStorage(
    private val database: MainDatabase
) {

    fun getLastBlockHeight(): Long? {
        return database.lastBlockHeightDao().getLastBlockHeight()?.height
    }

    fun saveLastBlockHeight(lastBlockHeight: Long) {
        database.lastBlockHeightDao().insert(LastBlockHeightEntity(lastBlockHeight))
    }

    fun saveBalance(balance: Long) {
        database.balanceDao().insert(BalanceEntity(balance))
    }

    fun getBalance(): Long? {
        return database.balanceDao().getBalance()?.lamports
    }

    fun saveInitialSync() {
        database.initialSyncDao().insert(InitialSyncEntity(initial = true))
    }

    fun isInitialSync(): Boolean {
        return database.initialSyncDao().getAllEntities().isEmpty()
    }

}
