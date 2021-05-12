package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.ActiveAccount
import io.reactivex.Flowable

class AccountsStorage(appDatabase: AppDatabase) : IAccountsStorage {

    private val dao: AccountsDao by lazy {
        appDatabase.accountsDao()
    }

    companion object {
        // account type codes stored in db
        private const val MNEMONIC = "mnemonic"
        private const val PRIVATE_KEY = "private_key"
    }

    override var activeAccountId: String?
        get() = dao.getActiveAccount()?.accountId
        set(value) {
            if (value != null) {
                dao.insertActiveAccount(ActiveAccount(value))
            } else {
                dao.deleteActiveAccount()
            }
        }

    override val isAccountsEmpty: Boolean
        get() = dao.getTotalCount() == 0

    override fun allAccounts(): List<Account> {
        return dao.getAll()
                .mapNotNull { record ->
                    try {
                        val accountType = when (record.type) {
                            MNEMONIC -> AccountType.Mnemonic(record.words!!.list, record.passphrase?.value ?: "")
                            PRIVATE_KEY -> AccountType.PrivateKey(record.key!!.value.hexToByteArray())
                            else -> null
                        }
                        Account(record.id, record.name, accountType!!, AccountOrigin.valueOf(record.origin), record.isBackedUp)
                    } catch (ex: Exception) {
                        null
                    }
                }
    }

    override fun getDeletedAccountIds(): List<String> {
        return dao.getDeletedIds()
    }

    override fun clearDeleted() {
        return dao.clearDeleted()
    }

    override fun save(account: Account) {
        dao.insert(getAccountRecord(account))
    }

    override fun update(account: Account) {
        dao.update(getAccountRecord(account))
    }

    override fun delete(id: String) {
        dao.delete(id)
    }

    override fun getNonBackedUpCount(): Flowable<Int> {
        return dao.getNonBackedUpCount()
    }

    override fun clear() {
        dao.deleteAll()
    }

    private fun getAccountRecord(account: Account): AccountRecord {
        var words: SecretList? = null
        var passphrase: SecretString? = null
        var key: SecretString? = null
        val accountType: String

        when (account.type) {
            is AccountType.Mnemonic -> {
                words = SecretList(account.type.words)
                passphrase = SecretString(account.type.passphrase)
                accountType = MNEMONIC
            }
            is AccountType.PrivateKey -> {
                key = SecretString(account.type.key.toRawHexString())
                accountType = PRIVATE_KEY
            }
            else -> throw Exception("Unsupported AccountType: ${account.type}")
        }

        return AccountRecord(
                id = account.id,
                name = account.name,
                type = accountType,
                origin = account.origin.value,
                isBackedUp = account.isBackedUp,
                words = words,
                passphrase = passphrase,
                key = key
        )
    }

}
