package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_53_54 : Migration(53, 54) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `ChartIndicatorSetting` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `index` INTEGER NOT NULL, `extraData` TEXT NOT NULL, `defaultData` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}
