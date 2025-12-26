package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_66_67 : Migration(66, 67) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE `RestoreSettingRecord` SET value = '3480000' WHERE blockchainTypeUid='monero' AND key='BirthdayHeight' AND value='-1'")

        try {
            db.execSQL(
                " INSERT INTO `RestoreSettingRecord`(`accountId`, `blockchainTypeUid`, `key`, `value`) " +
                        "SELECT `id`,  'monero', 'BirthdayHeight', '3480000' FROM AccountRecord WHERE type='mnemonic' AND origin='Created'"
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
