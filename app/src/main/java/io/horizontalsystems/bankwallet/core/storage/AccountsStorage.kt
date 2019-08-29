package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.reactivex.Flowable

class AccountsStorage(appDatabase: AppDatabase) : IAccountsStorage {

    private val dao = appDatabase.accountsDao()

    companion object {
        // account type codes stored in db
        private const val MNEMONIC = "mnemonic"
        private const val PRIVATE_KEY = "private_key"
        private const val HD_MASTER_KEY = "hd_master_key"
        private const val EOS = "eos"
    }

    override val isAccountsEmpty: Boolean
        get() = dao.getTotalCount() == 0

    override fun allAccounts(): List<Account> {
        return dao.getAll()
                .mapNotNull { record ->
                    try {
                        val accountType = when (record.type) {
                            MNEMONIC -> AccountType.Mnemonic(record.words!!.list, record.derivation!!, record.salt?.value)
                            PRIVATE_KEY -> AccountType.PrivateKey(record.key!!.value.hexToByteArray())
                            HD_MASTER_KEY -> AccountType.HDMasterKey(record.key!!.value.hexToByteArray(), record.derivation!!)
                            EOS -> AccountType.Eos(record.eosAccount!!, record.key!!.value)
                            else -> null
                        }
                        Account(record.id, record.name, accountType!!, record.isBackedUp, record.syncMode)
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
            is AccountType.HDMasterKey,
            is AccountType.Eos -> {
                AccountRecord(
                        id = account.id,
                        name = account.name,
                        type = getAccountTypeCode(account.type),
                        isBackedUp = account.isBackedUp,
                        syncMode = account.defaultSyncMode,
                        words = if (account.type is AccountType.Mnemonic) SecretList(account.type.words) else null,
                        derivation = getDerivation(account.type),
                        salt = if (account.type is AccountType.Mnemonic) account.type.salt?.let { SecretString(it) } else null,
                        key = getKey(account.type)?.let { SecretString(it) },
                        eosAccount = if (account.type is AccountType.Eos) account.type.account else null
                )
            }
            else -> throw Exception("Cannot save account with type: ${account.type}")
        }
    }

    private fun getKey(accountType: AccountType): String? {
        return when (accountType) {
            is AccountType.PrivateKey -> accountType.key.toHexString()
            is AccountType.HDMasterKey -> accountType.key.toHexString()
            is AccountType.Eos -> accountType.activePrivateKey
            else -> null
        }
    }

    private fun getDerivation(accountType: AccountType): AccountType.Derivation? {
        return when (accountType) {
            is AccountType.Mnemonic -> accountType.derivation
            is AccountType.HDMasterKey -> accountType.derivation
            else -> null
        }
    }

    private fun getAccountTypeCode(accountType: AccountType): String {
        return when (accountType) {
            is AccountType.HDMasterKey -> HD_MASTER_KEY
            is AccountType.PrivateKey -> PRIVATE_KEY
            is AccountType.Eos -> EOS
            else -> MNEMONIC
        }
    }

}
