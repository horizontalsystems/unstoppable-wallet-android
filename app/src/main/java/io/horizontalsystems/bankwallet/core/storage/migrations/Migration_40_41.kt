package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode

object Migration_40_41 : Migration(40, 41) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val btcRestoreKey = BlockchainSettingsStorage.keyBtcRestore
        database.execSQL("CREATE TABLE IF NOT EXISTS `BlockchainSettingRecord` (`blockchainUid` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`blockchainUid`, `key`))")

        val cursor = database.query("SELECT * FROM BlockchainSetting")
        while (cursor.moveToNext()) {
            val coinTypeColumnIndex = cursor.getColumnIndex("coinType")
            val keyColumnIndex = cursor.getColumnIndex("key")
            val valueColumnIndex = cursor.getColumnIndex("value")

            if (coinTypeColumnIndex >= 0 && keyColumnIndex >= 0 && valueColumnIndex >= 0) {
                val coinType = cursor.getString(coinTypeColumnIndex)
                val key = cursor.getString(keyColumnIndex)
                val value = cursor.getString(valueColumnIndex)

                if (key == "sync_mode") {
                    val btcBlockchain = when (coinType) {
                        "bitcoin" -> BtcBlockchain.Bitcoin
                        "bitcoinCash" -> BtcBlockchain.BitcoinCash
                        "litecoin" -> BtcBlockchain.Litecoin
                        "dash" -> BtcBlockchain.Dash
                        else -> null
                    }
                    val btcRestoreMode = when (value) {
                        "Slow" -> BtcRestoreMode.Blockchain
                        else -> BtcRestoreMode.Hybrid
                    }
                    btcBlockchain?.let { blockchain ->
                        database.execSQL(
                            """
                                INSERT INTO BlockchainSettingRecord (`blockchainUid`,`key`,`value`) 
                                VALUES ('${blockchain.raw}', '$btcRestoreKey', '${btcRestoreMode.raw}')
                                """.trimIndent()
                        )
                    }
                }

            }

        }
        database.execSQL("DROP TABLE BlockchainSetting")
        database.execSQL("DROP TABLE AccountSettingRecord")
    }
}

private enum class BtcBlockchain(val raw: String) {
    Bitcoin("bitcoin"),
    BitcoinCash("bitcoinCash"),
    Litecoin("litecoin"),
    Dash("dash");
}
