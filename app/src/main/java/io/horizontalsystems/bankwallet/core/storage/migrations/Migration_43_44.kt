package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_43_44 : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `SyncerState` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `EvmAddressLabel` (`address` TEXT NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`address`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `EvmMethodLabel` (`methodId` TEXT NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`methodId`))")
    }
}
