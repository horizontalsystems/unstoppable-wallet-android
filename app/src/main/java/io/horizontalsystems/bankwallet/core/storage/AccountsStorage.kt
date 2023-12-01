package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IAccountsStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.ActiveAccount
import io.horizontalsystems.bankwallet.entities.CexType
import io.reactivex.Flowable

class AccountsStorage(appDatabase: AppDatabase) : IAccountsStorage {

    private val dao: AccountsDao by lazy {
        appDatabase.accountsDao()
    }

    companion object {
        // account type codes stored in db
        private const val MNEMONIC = "mnemonic"
        private const val PRIVATE_KEY = "private_key"
        private const val ADDRESS = "address"
        private const val SOLANA_ADDRESS = "solana_address"
        private const val TRON_ADDRESS = "tron_address"
        private const val TON_ADDRESS = "ton_address"
        private const val BITCOIN_ADDRESS = "bitcoin_address"
        private const val HD_EXTENDED_LEY = "hd_extended_key"
        private const val CEX = "cex"
    }

    override fun getActiveAccountId(level: Int): String? {
        return dao.getActiveAccount(level)?.accountId
    }

    override fun setActiveAccountId(level: Int, id: String?) {
        if (id == null) {
            dao.deleteActiveAccount(level)
        } else {
            dao.insertActiveAccount(ActiveAccount(level, id))
        }
    }

    override val isAccountsEmpty: Boolean
        get() = dao.getTotalCount() == 0

    override fun allAccounts(accountsMinLevel: Int): List<Account> {
        return dao.getAll(accountsMinLevel)
                .mapNotNull { record: AccountRecord ->
                    try {
                        val accountType = when (record.type) {
                            MNEMONIC -> AccountType.Mnemonic(record.words!!.list, record.passphrase?.value ?: "")
                            PRIVATE_KEY -> AccountType.EvmPrivateKey(record.key!!.value.toBigInteger())
                            ADDRESS -> AccountType.EvmAddress(record.key!!.value)
                            SOLANA_ADDRESS -> AccountType.SolanaAddress(record.key!!.value)
                            TRON_ADDRESS -> AccountType.TronAddress(record.key!!.value)
                            TON_ADDRESS -> AccountType.TonAddress(record.key!!.value)
                            BITCOIN_ADDRESS -> AccountType.BitcoinAddress.fromSerialized(record.key!!.value)
                            HD_EXTENDED_LEY -> AccountType.HdExtendedKey(record.key!!.value)
                            CEX -> {
                                CexType.deserialize(record.key!!.value)?.let {
                                    AccountType.Cex(it)
                                }
                            }
                            else -> null
                        }
                        Account(
                            record.id,
                            record.name,
                            accountType!!,
                            AccountOrigin.valueOf(record.origin),
                            record.level,
                            record.isBackedUp,
                            record.isFileBackedUp
                        )
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

    override fun updateLevels(accountIds: List<String>, level: Int) {
        dao.updateLevels(accountIds, level)
    }

    override fun updateMaxLevel(level: Int) {
        dao.updateMaxLevel(level)
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
            is AccountType.EvmPrivateKey -> {
                key = SecretString(account.type.key.toString())
                accountType = PRIVATE_KEY
            }
            is AccountType.EvmAddress -> {
                key = SecretString(account.type.address)
                accountType = ADDRESS
            }
            is AccountType.SolanaAddress -> {
                key = SecretString(account.type.address)
                accountType = SOLANA_ADDRESS
            }
            is AccountType.TronAddress -> {
                key = SecretString(account.type.address)
                accountType = TRON_ADDRESS
            }
            is AccountType.TonAddress -> {
                key = SecretString(account.type.address)
                accountType = TON_ADDRESS
            }
            is AccountType.BitcoinAddress -> {
                key = SecretString(account.type.serialized)
                accountType = BITCOIN_ADDRESS
            }
            is AccountType.HdExtendedKey -> {
                key = SecretString(account.type.keySerialized)
                accountType = HD_EXTENDED_LEY
            }
            is AccountType.Cex -> {
                key = SecretString(account.type.cexType.serialized())
                accountType = CEX
            }
        }

        return AccountRecord(
            id = account.id,
            name = account.name,
            type = accountType,
            origin = account.origin.value,
            isBackedUp = account.isBackedUp,
            isFileBackedUp = account.isFileBackedUp,
            words = words,
            passphrase = passphrase,
            key = key,
            level = account.level
        )
    }

}
