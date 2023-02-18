package io.horizontalsystems.marketkit.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


object Migration_6_7: Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `Coin` ADD COLUMN `priority` INTEGER NULL;")
        database.execSQL("UPDATE `Coin` set `priority`=100;")
        database.execSQL("UPDATE `Coin` set `priority`=2 where `uid`='piratecash';")
        database.execSQL("UPDATE `Coin` set `priority`=1 where `uid`='cosanta';")
    }
}
