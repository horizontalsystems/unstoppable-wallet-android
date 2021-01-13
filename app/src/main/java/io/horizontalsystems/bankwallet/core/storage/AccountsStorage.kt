package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.reactivex.Flowable

class AccountsStorage(appDatabase: AppDatabase) : IAccountsStorage {

    private val dao: AccountsDao by lazy {
        appDatabase.accountsDao()
    }

    companion object {
        // account type codes stored in db
        private const val MNEMONIC = "mnemonic"
        private const val PRIVATE_KEY = "private_key"
        private const val ZCASH = "zcash"
    }

    override val isAccountsEmpty: Boolean
        get() = dao.getTotalCount() == 0

    override fun allAccounts(): List<Account> {
        return dao.getAll()
                .mapNotNull { record ->
                    try {
                        val accountType = when (record.type) {
                            MNEMONIC -> AccountType.Mnemonic(record.words!!.list, record.salt?.value)
                            PRIVATE_KEY -> AccountType.PrivateKey(record.key!!.value.hexToByteArray())
                            ZCASH -> AccountType.Zcash(record.words!!.list, record.birthdayHeight)
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
        return when (account.type) {
            is AccountType.Mnemonic,
            is AccountType.PrivateKey,
            is AccountType.Zcash -> {
                AccountRecord(
                        id = account.id,
                        name = account.name,
                        type = getAccountTypeCode(account.type),
                        origin = account.origin.value,
                        isBackedUp = account.isBackedUp,
                        words = when (account.type) {
                            is AccountType.Mnemonic -> SecretList(account.type.words)
                            is AccountType.Zcash -> SecretList(account.type.words)
                            else -> null
                        },
                        salt = (account.type as? AccountType.Mnemonic)?.salt?.let { SecretString(it) },
                        key = getKey(account.type)?.let { SecretString(it) },
                        birthdayHeight = (account.type as? AccountType.Zcash)?.birthdayHeight
                )
            }
            else -> throw Exception("Cannot save account with type: ${account.type}")
        }
    }

    private fun getKey(accountType: AccountType): String? {
        return when (accountType) {
            is AccountType.PrivateKey -> accountType.key.toRawHexString()
            else -> null
        }
    }

    private fun getAccountTypeCode(accountType: AccountType): String {
        return when (accountType) {
            is AccountType.PrivateKey -> PRIVATE_KEY
            is AccountType.Zcash -> ZCASH
            else -> MNEMONIC
        }
    }

}
