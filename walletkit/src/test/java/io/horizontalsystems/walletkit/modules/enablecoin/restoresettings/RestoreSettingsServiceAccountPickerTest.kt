package io.horizontalsystems.walletkit.modules.enablecoin.restoresettings

import androidx.compose.runtime.Composable
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.IRestoreSettingsStorage
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestoreSettingsServiceAccountPickerTest {

    private object PickerPage : HSPage() {
        @Composable
        override fun GetContent(navigation: HSNavigation) = Unit
    }

    private class FakeNearPlugin : ChainPlugin {
        override val blockchainType = BlockchainType.Near
        override fun clearAccountData(accountId: String) = Unit
        override fun accountPickerPage(accountType: AccountType): HSPage? =
            if (accountType is AccountType.Mnemonic) PickerPage else null
    }

    private class FakeStorage : IRestoreSettingsStorage {
        val records = mutableListOf<RestoreSettingRecord>()
        override fun restoreSettings(accountId: String, blockchainTypeUid: String) =
            records.filter { it.accountId == accountId && it.blockchainTypeUid == blockchainTypeUid }
        override fun restoreSettings(accountId: String) = records.filter { it.accountId == accountId }
        override fun save(restoreSettingRecords: List<RestoreSettingRecord>) {
            records.addAll(restoreSettingRecords)
        }
        override fun deleteAllRestoreSettings(accountId: String) {
            records.removeAll { it.accountId == accountId }
        }
    }

    private val near = Token(
        coin = Coin("near", "NEAR", "NEAR"),
        blockchain = Blockchain(BlockchainType.Near, "NEAR", null),
        type = TokenType.Native,
        decimals = 24,
    )
    private val mnemonic = AccountType.Mnemonic(List(11) { "abandon" } + "about", "")

    private lateinit var storage: FakeStorage
    private lateinit var service: RestoreSettingsService
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val requests = mutableListOf<RestoreSettingsService.Request>()
    private val approved = mutableListOf<RestoreSettingsService.TokenWithSettings>()

    @Before
    fun setUp() {
        ChainRegistry.register(FakeNearPlugin())
        storage = FakeStorage()
        service = RestoreSettingsService(RestoreSettingsManager(storage))
        scope.launch { service.requestFlow.collect { requests += it } }
        scope.launch { service.approveSettingsFlow.collect { approved += it } }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun account(origin: AccountOrigin, type: AccountType = mnemonic) =
        Account("id", "Wallet", type, origin, level = 0)

    @Test
    fun restoreFlow_mnemonic_asksForAccount() {
        service.approveSettings(near, accountType = mnemonic)

        assertEquals(RestoreSettingsService.RequestType.AccountPicker, requests.single().requestType)
        assertSame(PickerPage, requests.single().page)
        assertTrue(approved.isEmpty())
    }

    @Test
    fun restoredAccount_withoutChoice_asksForAccount() {
        service.approveSettings(near, account(AccountOrigin.Restored))

        assertEquals(RestoreSettingsService.RequestType.AccountPicker, requests.single().requestType)
    }

    @Test
    fun restoredAccount_withChoice_doesNotAskAgain() {
        storage.save(listOf(RestoreSettingRecord("id", BlockchainType.Near.uid, RestoreSettingType.NearAccountId.name, "alice.near")))

        service.approveSettings(near, account(AccountOrigin.Restored))

        assertTrue(requests.isEmpty())
        assertEquals(1, approved.size)
    }

    // A phrase made in this app has no named accounts yet
    @Test
    fun createdAccount_doesNotAsk() {
        service.approveSettings(near, account(AccountOrigin.Created))

        assertTrue(requests.isEmpty())
        assertTrue(approved.single().settings.values.isEmpty())
    }

    @Test
    fun watchAccount_doesNotAsk() {
        service.approveSettings(near, account(AccountOrigin.Restored, AccountType.NearAddress("alice.near")))

        assertTrue(requests.isEmpty())
        assertEquals(1, approved.size)
    }

    @Test
    fun pickerResult_isApprovedForTheToken() {
        val settings = RestoreSettings().apply { this[RestoreSettingType.NearAccountId] = "alice.near" }

        service.enter(settings, near)

        assertEquals(near, approved.single().token)
        assertEquals("alice.near", approved.single().settings.nearAccountId)
    }
}
