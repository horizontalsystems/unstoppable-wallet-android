package cash.p.terminal.wallet

import io.reactivex.Flowable

interface IAccountsStorage {
    val isAccountsEmpty: Boolean

    fun getActiveAccountId(level: Int): String?
    fun setActiveAccountId(level: Int, id: String?)
    fun allAccounts(accountsMinLevel: Int): List<Account>
    fun loadAccount(id: String): Account?
    fun save(account: Account)
    fun update(account: Account)
    fun delete(id: String)
    fun getNonBackedUpCount(): Flowable<Int>
    fun clear()
    fun getDeletedAccountIds(): List<String>
    fun clearDeleted()
    fun updateLevels(accountIds: List<String>, level: Int)
    fun updateMaxLevel(level: Int)
}
