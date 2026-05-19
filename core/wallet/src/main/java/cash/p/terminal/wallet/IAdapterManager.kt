package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.core.entities.BlockchainType
import io.reactivex.Flowable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

interface IAdapterManager {
    val adaptersReadyObservable: Flowable<Map<Wallet, IAdapter>>
    val initializationInProgressFlow: StateFlow<Boolean>
    val walletBalanceUpdatedFlow: SharedFlow<Wallet>

    fun startAdapterManager()
    suspend fun refresh()

    /**
     * Wait for adapter for the given wallet to be available
     * If adapter is already available, return it
     * @param timeoutMs Maximum time to wait for adapter initialization (default 300ms)
     */
    suspend fun <T> awaitAdapterForWallet(wallet: Wallet, timeoutMs: Long = 300): T?

    fun <T> getAdapterForWallet(wallet: Wallet): T?
    fun getAdapterForWalletOld(wallet: Wallet): IAdapter?
    fun <T> getAdapterForToken(token: Token): T?
    fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    fun getReceiveAdapterForWallet(wallet: Wallet): IReceiveAdapter?
    fun refreshAdapters(wallets: List<Wallet>)
    fun refreshByWallet(wallet: Wallet)
    suspend fun stopAdapters(accountIds: List<String>) = Unit
    suspend fun stopAdapters(accountIds: List<String>, blockchainType: BlockchainType) = Unit

    /**
     * Get balance data adjusted for pending transactions.
     * Automatically handles different SDK behaviors (some deduct immediately, some don't).
     */
    fun getAdjustedBalanceData(wallet: Wallet): BalanceData?

    /**
     * Get balance data adjusted for pending transactions by token.
     * Finds the active wallet for the token and returns adjusted balance.
     */
    fun getAdjustedBalanceDataForToken(token: Token): BalanceData?

    /**
     * Get receive address with fallback when adapter not available.
     * First tries adapter, then fallback providers.
     */
    suspend fun getReceiveAddressForWallet(wallet: Wallet): String?
}

/**
 * Get max sendable balance considering both:
 * 1. Adjusted balance (pending transactions subtracted)
 * 2. Adapter's available balance (fees subtracted for some chains)
 * Returns the minimum of both when adjusted data is available.
 */
fun IAdapterManager.getMaxSendableBalance(wallet: Wallet, adapterAvailableBalance: BigDecimal): BigDecimal {
    val adjusted = getAdjustedBalanceData(wallet)?.available
    return if (adjusted != null) {
        minOf(adjusted, adapterAvailableBalance)
    } else {
        adapterAvailableBalance
    }
}
