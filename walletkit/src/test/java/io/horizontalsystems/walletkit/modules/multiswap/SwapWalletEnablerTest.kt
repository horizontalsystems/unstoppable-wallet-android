package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IRestoreSettingsStorage
import io.horizontalsystems.walletkit.core.IWalletStorage
import io.horizontalsystems.walletkit.core.managers.ActiveAccountState
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.EnabledWallet
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord
import io.horizontalsystems.walletkit.entities.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapWalletEnablerTest {

    private companion object {
        const val NEW_WALLET_HEIGHT = 3_000_000L
    }

    private fun token(blockchainType: BlockchainType) = Token(
        coin = Coin("uid-${blockchainType.uid}", "Name", "CODE"),
        blockchain = Blockchain(blockchainType, "Name", null),
        type = TokenType.Native,
        decimals = 8,
    )

    private fun account(origin: AccountOrigin) = Account(
        id = "account",
        name = "Account",
        type = AccountType.Mnemonic(List(12) { "word" }, ""),
        origin = origin,
        level = 0,
    )

    private val sol = token(BlockchainType.Solana)
    private val zcash = token(BlockchainType.Zcash)

    private class Fixture(account: Account, newWalletHeight: Long? = NEW_WALLET_HEIGHT) {
        val walletStorage = FakeWalletStorage()
        val restoreSettingsStorage = FakeRestoreSettingsStorage()
        val walletManager = WalletManager(FakeAccountManager(account), walletStorage)
        val enabler = SwapWalletEnabler(
            walletManager,
            RestoreSettingsManager(restoreSettingsStorage),
            newWalletSettingValue = { _, _ -> newWalletHeight?.toString() },
        )

        fun savedBirthdayHeight(blockchainType: BlockchainType) = restoreSettingsStorage
            .restoreSettings("account", blockchainType.uid)
            .firstOrNull { it.key == RestoreSettingType.BirthdayHeight.name }
            ?.value
    }

    @Test
    fun `enables the wallet of the bought token`() {
        val account = account(AccountOrigin.Restored)
        val fixture = Fixture(account)

        assertTrue(fixture.enabler.enableReceivingWallet(account, sol))

        assertEquals(listOf(Wallet(sol, account)), fixture.walletStorage.saved)
        assertTrue(fixture.walletManager.activeWallets.contains(Wallet(sol, account)))
    }

    @Test
    fun `leaves an already enabled wallet alone`() {
        val account = account(AccountOrigin.Restored)
        val fixture = Fixture(account)
        fixture.walletManager.save(listOf(Wallet(sol, account)))
        fixture.walletStorage.saved.clear()

        assertFalse(fixture.enabler.enableReceivingWallet(account, sol))

        assertTrue(fixture.walletStorage.saved.isEmpty())
    }

    @Test
    fun `enables a birthday-height chain with the new-wallet height when none is saved`() {
        val account = account(AccountOrigin.Restored)
        val fixture = Fixture(account)

        assertTrue(fixture.enabler.enableReceivingWallet(account, zcash))

        assertEquals(listOf(Wallet(zcash, account)), fixture.walletStorage.saved)
        assertEquals(NEW_WALLET_HEIGHT.toString(), fixture.savedBirthdayHeight(BlockchainType.Zcash))
    }

    @Test
    fun `keeps a saved birthday height`() {
        val account = account(AccountOrigin.Restored)
        val fixture = Fixture(account)
        fixture.restoreSettingsStorage.save(
            listOf(
                RestoreSettingRecord(
                    account.id,
                    BlockchainType.Zcash.uid,
                    RestoreSettingType.BirthdayHeight.name,
                    "2500000",
                )
            )
        )

        assertTrue(fixture.enabler.enableReceivingWallet(account, zcash))

        assertEquals(listOf(Wallet(zcash, account)), fixture.walletStorage.saved)
        assertEquals("2500000", fixture.savedBirthdayHeight(BlockchainType.Zcash))
    }

    @Test
    fun `enables a birthday-height chain even when no new-wallet height is available`() {
        val account = account(AccountOrigin.Created)
        val fixture = Fixture(account, newWalletHeight = null)

        assertTrue(fixture.enabler.enableReceivingWallet(account, zcash))

        assertEquals(listOf(Wallet(zcash, account)), fixture.walletStorage.saved)
        assertEquals(null, fixture.savedBirthdayHeight(BlockchainType.Zcash))
    }

    @Test
    fun `saves no restore settings for chains without them`() {
        val account = account(AccountOrigin.Restored)
        val fixture = Fixture(account)

        assertTrue(fixture.enabler.enableReceivingWallet(account, sol))

        assertEquals(null, fixture.savedBirthdayHeight(BlockchainType.Solana))
    }

    private class FakeWalletStorage : IWalletStorage {
        val saved = mutableListOf<Wallet>()

        override fun wallets(account: Account): List<Wallet> = saved.filter { it.account == account }
        override fun save(wallets: List<Wallet>) {
            saved.addAll(wallets)
        }

        override fun delete(wallets: List<Wallet>) {
            saved.removeAll(wallets)
        }

        override fun handle(newEnabledWallets: List<EnabledWallet>) = Unit
        override fun clear() = saved.clear()
    }

    private class FakeRestoreSettingsStorage : IRestoreSettingsStorage {
        private val records = mutableListOf<RestoreSettingRecord>()

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

    private class FakeAccountManager(override val activeAccount: Account?) : IAccountManager {
        override val hasNonStandardAccount = false
        override val activeAccountStateFlow: Flow<ActiveAccountState> = emptyFlow()
        override val isAccountsEmpty = false
        override val accounts = listOfNotNull(activeAccount)
        override val accountsFlow: Flow<List<Account>> = emptyFlow()
        override val accountsDeletedFlow: Flow<Unit> = emptyFlow()

        override fun setActiveAccountId(activeAccountId: String?) = Unit
        override fun account(id: String) = activeAccount?.takeIf { it.id == id }
        override fun save(account: Account) = Unit
        override fun import(accounts: List<Account>) = Unit
        override fun update(account: Account) = Unit
        override fun delete(id: String) = Unit
        override fun clear() = Unit
        override fun clearAccounts() = Unit
        override fun setLevel(level: Int) = Unit
        override fun updateAccountLevels(accountIds: List<String>, level: Int) = Unit
        override fun updateMaxLevel(level: Int) = Unit
        override fun getRandomWalletName() = "Wallet"
    }
}
