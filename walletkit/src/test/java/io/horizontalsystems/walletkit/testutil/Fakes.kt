package io.horizontalsystems.walletkit.testutil

import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IRestoreSettingsStorage
import io.horizontalsystems.walletkit.core.IWalletStorage
import io.horizontalsystems.walletkit.core.managers.ActiveAccountState
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.EnabledWallet
import io.horizontalsystems.walletkit.entities.RestoreSettingRecord
import io.horizontalsystems.walletkit.entities.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeWalletStorage : IWalletStorage {
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

internal class FakeRestoreSettingsStorage : IRestoreSettingsStorage {
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

internal class FakeAccountManager(override val activeAccount: Account?) : IAccountManager {
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
