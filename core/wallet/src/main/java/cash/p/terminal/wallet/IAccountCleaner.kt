package cash.p.terminal.wallet

interface IAccountCleaner {
    suspend fun clearAccounts(accountIds: List<String>)
    suspend fun clearWalletForAccount(accountId: String, token: Token)
    suspend fun clearWalletForCurrentAccount(token: Token)
}
