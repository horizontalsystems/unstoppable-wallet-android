package io.horizontalsystems.walletkit.modules.backuplocal

import com.google.gson.GsonBuilder
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.modules.backuplocal.fullbackup.ZanoNodes
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

    @Test
    fun zanoAutoSelect_isWrittenUnderItsSerializedName() {
        val nodes = ZanoNodes(selected = emptyList(), custom = emptyList(), autoSelect = false)

        val json = gson.toJson(nodes)

        assertTrue(json, json.contains("\"auto_select\":false"))
    }

    // A backup made before the setting existed must not decide it on restore
    @Test
    fun zanoAutoSelect_isNullInAnOlderBackup() {
        val json = """{"selected":[{"blockchain_type_id":"zano","url":"https://node.zano.org:443"}],"custom":[]}"""

        val nodes = gson.fromJson(json, ZanoNodes::class.java)

        assertEquals(null, nodes.autoSelect)
        assertEquals("https://node.zano.org:443", nodes.selected.single().url)
    }
}
