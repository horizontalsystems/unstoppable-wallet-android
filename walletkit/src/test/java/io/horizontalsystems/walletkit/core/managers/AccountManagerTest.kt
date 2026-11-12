package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountCleaner
import io.horizontalsystems.walletkit.core.IAccountsStorage
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AccountManagerTest {

    private class Storage(private val failOnSave: Set<String> = emptySet()) : IAccountsStorage {
        val accounts = mutableMapOf<String, Account>()
        private var activeAccountId: String? = null

        override val isAccountsEmpty get() = accounts.isEmpty()
        override fun getActiveAccountId(level: Int) = activeAccountId
        override fun setActiveAccountId(level: Int, id: String?) {
            activeAccountId = id
        }

        override fun allAccounts(accountsMinLevel: Int) = accounts.values.toList()
        override fun save(account: Account) {
            check(account.id !in failOnSave) { "Saving ${account.id} failed" }
            accounts[account.id] = account
        }

        override fun update(account: Account) {
            accounts[account.id] = account
        }

        override fun delete(id: String) {
            accounts.remove(id)
        }

        override fun clear() = accounts.clear()
        override fun getDeletedAccountIds() = emptyList<String>()
        override fun clearDeleted() = Unit
        override fun updateLevels(accountIds: List<String>, level: Int) = Unit
        override fun updateMaxLevel(level: Int) = Unit
    }

    private class Fixture(failOnSave: Set<String> = emptySet()) {
        var walletsAdded = 0
        val manager = AccountManager(
            storage = Storage(failOnSave),
            accountCleaner = object : IAccountCleaner {
                override fun clearAccounts(accountIds: List<String>) = Unit
            },
            onWalletAdded = { walletsAdded++ },
        )
    }

    private fun account(id: String) = Account(
        id = id,
        name = "Wallet $id",
        type = AccountType.Mnemonic(List(12) { "word" }, ""),
        origin = AccountOrigin.Created,
        level = 0,
    )

    @Test
    fun save_reportsWalletAdded() {
        val fixture = Fixture()

        fixture.manager.save(account("a"))

        assertEquals(1, fixture.walletsAdded)
    }

    @Test
    fun save_failing_reportsNothing() {
        val fixture = Fixture(failOnSave = setOf("a"))

        assertThrows(IllegalStateException::class.java) {
            fixture.manager.save(account("a"))
        }

        assertEquals(0, fixture.walletsAdded)
    }

    @Test
    fun import_reportsWalletAdded() {
        val fixture = Fixture()

        fixture.manager.import(listOf(account("a"), account("b")))

        assertEquals(1, fixture.walletsAdded)
    }

    @Test
    fun import_ofNoAccounts_reportsNothing() {
        val fixture = Fixture()

        fixture.manager.import(emptyList())

        assertEquals(0, fixture.walletsAdded)
    }

    @Test
    fun import_failingAfterTheFirstWallet_reportsWalletAdded() {
        val fixture = Fixture(failOnSave = setOf("b"))

        assertThrows(IllegalStateException::class.java) {
            fixture.manager.import(listOf(account("a"), account("b")))
        }

        assertEquals(1, fixture.walletsAdded)
    }

    @Test
    fun import_failingOnTheFirstWallet_reportsNothing() {
        val fixture = Fixture(failOnSave = setOf("a"))

        assertThrows(IllegalStateException::class.java) {
            fixture.manager.import(listOf(account("a"), account("b")))
        }

        assertEquals(0, fixture.walletsAdded)
    }

    @Test
    fun changingOrRemovingWallets_reportsNothing() {
        val fixture = Fixture()
        fixture.manager.save(account("a"))
        fixture.walletsAdded = 0

        fixture.manager.update(account("a").copy(name = "Renamed"))
        fixture.manager.setLevel(0)
        fixture.manager.delete("a")
        fixture.manager.clear()

        assertEquals(0, fixture.walletsAdded)
    }
}
