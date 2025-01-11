package cash.p.terminal.wallet

interface IAccountCleaner {
    fun clearAccounts(accountIds: List<String>)
}