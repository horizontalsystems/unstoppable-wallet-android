package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_66_67 : Migration(66, 67) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `HardwarePublicKey` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` TEXT NOT NULL,
                `blockchainType` TEXT NOT NULL,
                `key` TEXT NOT NULL,
                FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_HardwarePublicKey_accountId` ON `HardwarePublicKey` (`accountId`)")
    }
}
