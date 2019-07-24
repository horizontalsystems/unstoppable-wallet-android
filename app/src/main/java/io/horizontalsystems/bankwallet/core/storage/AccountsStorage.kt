package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.reactivex.Flowable
import java.util.concurrent.Executors

class AccountsStorage(appDatabase: AppDatabase) : IAccountsStorage {

    private val executor = Executors.newSingleThreadExecutor()
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
                            MNEMONIC -> AccountType.Mnemonic(record.words!!.list, record.derivation!!, record.salt!!.value)
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

    override fun save(account: Account) {
        val accountTypeCode: String
        var words: List<String>? = null
        var derivation: AccountType.Derivation? = null
        var salt: String? = null
        var key: String? = null
        var eosAccount: String? = null

        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                accountTypeCode = MNEMONIC
                words = accountType.words
                derivation = accountType.derivation
                salt = accountType.salt
            }
            is AccountType.PrivateKey -> {
                accountTypeCode = PRIVATE_KEY
                key = accountType.key.toHexString()

            }
            is AccountType.HDMasterKey -> {
                accountTypeCode = HD_MASTER_KEY
                key = accountType.key.toHexString()
                derivation = accountType.derivation
            }
            is AccountType.Eos -> {
                accountTypeCode = EOS
                key = accountType.activePrivateKey
                eosAccount = accountType.account
            }
            else -> throw Exception("Cannot save account with type: $accountType")
        }

        executor.execute {
            dao.insert(AccountRecord(account.id,
                    account.name,
                    accountTypeCode,
                    account.isBackedUp,
                    account.defaultSyncMode,
                    words?.let { SecretList(it) },
                    derivation,
                    salt?.let { SecretString(it) },
                    key?.let { SecretString(it) },
                    eosAccount))
        }
    }

    override fun delete(id: String) {
        executor.execute {
            dao.delete(id)
        }
    }

    override fun getNonBackedUpCount(): Flowable<Int> {
        return dao.getNonBackedUpCount()
    }

    override fun clear() {
        dao.deleteAll()
    }

}
