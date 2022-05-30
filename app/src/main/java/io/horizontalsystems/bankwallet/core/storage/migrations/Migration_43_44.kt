package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_43_44 : Migration(43, 44) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `SyncerState` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
        database.execSQL("CREATE TABLE IF NOT EXISTS `EvmAddressLabel` (`address` TEXT NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`address`))")
        database.execSQL("CREATE TABLE IF NOT EXISTS `EvmMethodLabel` (`methodId` TEXT NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`methodId`))")
    }
}
