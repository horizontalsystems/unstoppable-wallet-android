package cash.p.terminal.wallet

import android.content.Context
import cash.p.terminal.core.storage.AccountsDao
import cash.p.terminal.core.storage.AccountsStorage
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.storage.UserDeletedWalletDao
import cash.p.terminal.entities.ActiveAccount
import cash.p.terminal.wallet.entities.AccountRecord
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import cash.p.terminal.wallet.useCases.IGetMoneroWalletFilesNameUseCase
import cash.p.terminal.wallet.useCases.RemoveMoneroWalletFilesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.reactivex.Flowable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HiddenWalletAccountVisibilityTest {

    private val accountsDao = InMemoryAccountsDao()
    private val storage = AccountsStorage(FakeAppDatabase(accountsDao))
    private val getMoneroWalletFilesNameUseCase = mockk<IGetMoneroWalletFilesNameUseCase>()
    private val removeMoneroWalletFilesUseCase: RemoveMoneroWalletFilesUseCase =
        RemoveMoneroWalletFilesUseCase(
            appContext = mockk<Context>(relaxed = true),
            getMoneroWalletFilesNameUseCase = getMoneroWalletFilesNameUseCase
        )
    private val balanceHiddenManager = mockk<IBalanceHiddenManager>(relaxed = true)
    private val accountManager = AccountManager(
        storage = storage,
        getMoneroWalletFilesNameUseCase = getMoneroWalletFilesNameUseCase,
        removeMoneroWalletFilesUseCase = removeMoneroWalletFilesUseCase,
        balanceHiddenManager = balanceHiddenManager
    )

    @Before
    fun setup() {
        accountsDao.reset()
        listOf(
            testAccount(id = "regular", level = 0),
            testAccount(id = "duress", level = 1),
            testAccount(id = "hidden-1", level = -1),
            testAccount(id = "hidden-2", level = -2)
        ).forEach { storage.save(it) }
        coEvery { getMoneroWalletFilesNameUseCase.invoke(any()) } returns null
    }

    @Test
    fun `hidden wallets are excluded from regular level`() {
        accountManager.setLevel(0)

        val ids = accountManager.accounts.map { it.id }

        assertTrue(ids.contains("regular"))
        assertTrue(ids.contains("duress"))
        assertTrue(ids.none { it.startsWith("hidden") })
    }

    @Test
    fun `hidden wallets are excluded from duress level`() {
        accountManager.setLevel(1)

        val ids = accountManager.accounts.map { it.id }

        assertEquals(listOf("duress"), ids)
    }

    @Test
    fun `hidden level exposes only its own wallets`() {
        accountManager.setLevel(-2)

        val ids = accountManager.accounts.map { it.id }

        assertEquals(listOf("hidden-2"), ids)
    }

    @Test
    fun `accounts storage counts wallets per hidden level`() {
        assertEquals(1, storage.getWalletsCountByLevel(-1))
        assertEquals(1, storage.getWalletsCountByLevel(-2))
        assertEquals(0, storage.getWalletsCountByLevel(-99))
    }

    private fun testAccount(id: String, level: Int): Account {
        return Account(
            id = id,
            name = id,
            type = AccountType.EvmAddress(id),
            origin = AccountOrigin.Restored,
            level = level,
            isBackedUp = true
        )
    }
}

private class FakeAppDatabase(
    private val accountsDao: AccountsDao
) : AppDatabase() {

    override fun accountsDao(): AccountsDao = accountsDao

    override fun chartIndicatorSettingsDao() = unsupported()
    override fun walletsDao() = unsupported()
    override fun enabledWalletsCacheDao() = unsupported()
    override fun blockchainSettingDao() = unsupported()
    override fun evmSyncSourceDao() = unsupported()
    override fun restoreSettingDao() = unsupported()
    override fun logsDao() = unsupported()
    override fun marketFavoritesDao() = unsupported()
    override fun wcSessionDao() = unsupported()
    override fun nftDao() = unsupported()
    override fun evmAddressLabelDao() = unsupported()
    override fun evmMethodLabelDao() = unsupported()
    override fun syncerStateDao() = unsupported()
    override fun tokenAutoEnabledBlockchainDao() = unsupported()
    override fun spamAddressDao() = unsupported()
    override fun pinDao() = unsupported()
    override fun swapProviderTransactionsDao() = unsupported()
    override fun hardwarePublicKeyDao() = unsupported()
    override fun recentAddressDao() = unsupported()
    override fun moneroFileDao() = unsupported()
    override fun pendingMultiSwapDao() = unsupported()
    override fun pendingTransactionDao() = unsupported()
    override fun zcashSingleUseAddressDao() = unsupported()
    override fun userDeletedWalletDao() = unsupported()
    override fun poisonAddressDao() = unsupported()

    private fun unsupported(): Nothing = throw NotImplementedError()
    override fun createInvalidationTracker() = unsupported()

    override fun clearAllTables() = unsupported()
}

private class InMemoryAccountsDao : AccountsDao {
    private val accounts = linkedMapOf<String, AccountRecord>()
    private val active = mutableMapOf<Int, ActiveAccount>()

    fun reset() {
        accounts.clear()
        active.clear()
    }

    override fun insert(accountRow: AccountRecord) {
        accounts[accountRow.id] = accountRow.copy().also {
            it.deleted = accountRow.deleted
        }
    }

    override fun update(accountRow: AccountRecord) {
        insert(accountRow)
    }

    override fun loadAccount(id: String): AccountRecord? = accounts[id]?.copy()

    override fun delete(id: String) {
        accounts[id]?.let { accounts[id] = it.apply { deleted = true } }
    }

    override fun updateName(id: String, name: String) {
        accounts[id]?.let { accounts[id] = it.copy(name = name) }
    }

    override fun updateBackupFlags(id: String, isBackedUp: Boolean, isFileBackedUp: Boolean) {
        accounts[id]?.let { accounts[id] = it.copy(isBackedUp = isBackedUp, isFileBackedUp = isFileBackedUp) }
    }

    override fun updateLevels(accountIds: List<String>, level: Int) {
        accountIds.forEach { id ->
            accounts[id]?.let { record ->
                accounts[id] = record.copy(level = level)
            }
        }
    }

    override fun updateMaxLevel(level: Int) {
        accounts.values.forEach { record ->
            if (record.level > level) {
                accounts[record.id] = record.copy(level = level)
            }
        }
    }

    override fun getAll(accountsMinLevel: Int): List<AccountRecord> =
        accounts.values.filter { !it.deleted && it.level >= accountsMinLevel }.map { it.copy() }

    override fun getAccountForLevel(level: Int): List<AccountRecord> =
        accounts.values.filter { !it.deleted && it.level == level }.map { it.copy() }

    override fun getDeletedIds(): List<String> =
        accounts.values.filter { it.deleted }.map { it.id }

    override fun getNonBackedUpCount(): Flowable<Int> =
        Flowable.just(accounts.values.count { !it.isBackedUp && !it.deleted })

    override fun getTotalCount(): Int =
        accounts.values.count { !it.deleted }

    override fun deleteAll() {
        accounts.values.forEach { it.deleted = true }
    }

    override fun clearDeleted() {
        val iterator = accounts.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.deleted) {
                iterator.remove()
            }
        }
    }

    override fun getActiveAccount(level: Int): ActiveAccount? = active[level]

    override fun insertActiveAccount(activeAccount: ActiveAccount) {
        active[activeAccount.level] = activeAccount
    }

    override fun deleteActiveAccount(level: Int) {
        active.remove(level)
    }

    override fun getCountByLevel(level: Int): Int =
        accounts.values.count { !it.deleted && it.level == level }
}
