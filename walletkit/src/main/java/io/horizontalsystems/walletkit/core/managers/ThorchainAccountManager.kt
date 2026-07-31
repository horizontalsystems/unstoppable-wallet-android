package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.EnabledWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.thorchainkit.models.Denom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigInteger

// Auto-enables Thorchain tokens (e.g. TCY secured assets) that the restored address already
// holds, so they appear alongside native RUNE — the balance-driven analog of the
// transaction-driven EVM/Stellar account managers. The shared kit already fetches every
// denom balance (cosmos/bank/.../balances), so discovery is just observing balancesFlow.
class ThorchainAccountManager(
    private val accountManager: IAccountManager,
    private val walletManager: WalletManager,
    private val thorchainKitManager: ThorchainKitManager,
    private val marketKit: MarketKitWrapper,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
) {
    private val blockchainType = BlockchainType.Thorchain
    private val logger = AppLogger("thorchain-account-manager")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var balanceSubscriptionJob: Job? = null

    fun start() {
        coroutineScope.launch {
            thorchainKitManager.kitStartedFlow.collect { started ->
                handleStarted(started)
            }
        }
    }

    private fun handleStarted(started: Boolean) {
        try {
            if (started) {
                subscribeToBalances()
            } else {
                stop()
            }
        } catch (exception: Exception) {
            logger.warning("error", exception)
        }
    }

    private fun stop() {
        balanceSubscriptionJob?.cancel()
    }

    private fun subscribeToBalances() {
        val wrapper = thorchainKitManager.thorchainKitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        // The first synced (non-empty) balance snapshot is the "initial" one that carries the
        // restore-time auto-enable gate; balancesFlow is a StateFlow that starts at emptyMap.
        var initial = true
        balanceSubscriptionJob = coroutineScope.launch {
            wrapper.thorchainKit.balancesFlow.collect { balances ->
                // A throw here would escape the launch and cancel coroutineScope (a plain Job,
                // not a SupervisorJob), permanently killing the kitStartedFlow collector — so
                // swallow and log, letting collection continue.
                try {
                    if (balances.isEmpty()) return@collect

                    val isInitial = initial
                    initial = false
                    handle(balances, account, isInitial)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warning("error", e)
                }
            }
        }
    }

    private fun handle(balances: Map<String, BigInteger>, account: Account, initial: Boolean) {
        val shouldAutoEnableTokens = tokenAutoEnableManager.isAutoEnabled(account, blockchainType)

        if (initial && account.origin == AccountOrigin.Restored && !account.isWatchAccount && !shouldAutoEnableTokens) {
            return
        }

        // Native RUNE is enabled with the blockchain itself; here we only add held tokens.
        val denoms = balances.filter { it.value > BigInteger.ZERO && it.key != Denom.RUNE }.keys
        if (denoms.isEmpty()) return

        val queries = denoms.map { TokenQuery(blockchainType, TokenType.ThorchainAsset(it)) }

        val existingTokenTypeIds = walletManager.activeWallets.map { it.token.type.id }
        val newQueries = queries.filter { !existingTokenTypeIds.contains(it.tokenType.id) }
        if (newQueries.isEmpty()) return

        // Only tokens known to marketkit get enabled — resolves proper name/code/decimals/image.
        val tokens = marketKit.tokens(newQueries)

        val enabledWallets = tokens.map { token ->
            EnabledWallet(
                tokenQueryId = token.tokenQuery.id,
                accountId = account.id,
                coinName = token.coin.name,
                coinCode = token.coin.code,
                coinDecimals = token.decimals,
                coinImage = token.coin.image
            )
        }

        if (enabledWallets.isNotEmpty()) {
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }
}
