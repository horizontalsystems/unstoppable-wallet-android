package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_50_51 : Migration(46, 47) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `EvmAccountState`")
        database.execSQL("CREATE TABLE IF NOT EXISTS `TokenAutoEnabledBlockchain` (`accountId` TEXT NOT NULL, `blockchainType` TEXT NOT NULL, PRIMARY KEY(`accountId`, `blockchainType`))")
    }
}
