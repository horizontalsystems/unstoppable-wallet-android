package io.horizontalsystems.walletkit.modules.backuplocal

import com.google.gson.GsonBuilder
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreSettingsTest {

    // Same configuration as BackupProvider
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .enableComplexMapKeySerialization()
        .create()

    @Test
    fun nearAccountId_isWrittenUnderItsSerializedName() {
        val backup = BackupLocalModule.EnabledWalletBackup(
            tokenQueryId = "near-protocol|native",
            settings = mapOf(RestoreSettingType.NearAccountId to "alice.near"),
        )

        val json = gson.toJson(backup)

        assertTrue(json, json.contains("\"settings\":{\"near_account_id\":\"alice.near\"}"))
    }

    @Test
    fun nearAccountId_roundTrips() {
        val json = """{"token_query_id":"near-protocol|native","settings":{"near_account_id":"alice.near"}}"""

        val backup = gson.fromJson(json, BackupLocalModule.EnabledWalletBackup::class.java)

        assertEquals(mapOf(RestoreSettingType.NearAccountId to "alice.near"), backup.settings)
    }

    // An older version reads a newer setting type as a null key; restore must skip it, not crash
    @Test
    fun unknownSettingType_isReadAsNullKey() {
        val json = """{"token_query_id":"zcash|native","settings":{"birthday_height":"100","from_the_future":"x"}}"""

        val settings = gson.fromJson(json, BackupLocalModule.EnabledWalletBackup::class.java).settings

        assertEquals(mapOf<RestoreSettingType?, String>(RestoreSettingType.BirthdayHeight to "100", null to "x"), settings)
    }
}
