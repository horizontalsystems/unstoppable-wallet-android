package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.xrpkit.models.TrustLine
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.EnabledWallet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.Executors

/**
 * Enables a wallet for every issued token the account holds a positive balance of, so a
 * restored or freshly credited account shows its tokens without manual adding.
 */
class XrpAccountManager(
    private val accountManager: IAccountManager,
    private val walletManager: WalletManager,
    private val kitManager: XrpKitManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
) {
    private val blockchainType: BlockchainType = BlockchainType.Xrp
    private val logger = AppLogger("xrp-account-manager")
    private val singleDispatcherCoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var subscriptionJob: Job? = null

    fun start() {
        singleDispatcherCoroutineScope.launch {
            kitManager.kitStartedFlow.collect { started ->
                try {
                    if (started) subscribe() else subscriptionJob?.cancel()
                } catch (e: Exception) {
                    logger.warning("error", e)
                }
            }
        }
    }

    private fun subscribe() {
        val wrapper = kitManager.kitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        // kitStartedFlow is a StateFlow and may conflate a rapid false -> true, so the collector
        // for the previous kit has to be dropped here, not only on `false`.
        subscriptionJob?.cancel()
        subscriptionJob = coroutineScope.launch {
            wrapper.kit.trustLinesFlow.collect { lines ->
                // coroutineScope has a plain Job: a throw here would cancel it and permanently
                // kill the kitStartedFlow collector, so swallow and log.
                try {
                    handle(lines, account, wrapper.persistedTrustLines)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.warning("error", e)
                }
            }
        }
    }

    /**
     * [persisted] is what the kit had in storage when it was created, before any sync. Tokens
     * held there are enabled only when the user opted into XRP on the restore screen. Anything
     * synced since, i.e. the first sync of a kit enabled from Coin Manager or a token received
     * later, is auto-enabled as on every other chain. Keying on the baseline rather than on the
     * first emission keeps the gate independent of when this collector attaches.
     */
    private fun handle(lines: List<TrustLine>, account: Account, persisted: List<TrustLine>) {
        if (!tokenAutoEnableManager.autoEnableTokensOnReceive) return

        val gated = account.origin == AccountOrigin.Restored &&
            !account.isWatchAccount &&
            !tokenAutoEnableManager.isAutoEnabled(account, blockchainType)
        val persistedHeldIds = if (gated) persisted.filter { it.isHeld }.map { it.tokenTypeId }.toSet() else emptySet()

        val held = lines.filter { it.isHeld && it.tokenTypeId !in persistedHeldIds }
        if (held.isEmpty()) return

        val existingTokenTypeIds = walletManager.activeWallets.map { it.token.type.id }
        val newLines = held.filter { it.tokenTypeId !in existingTokenTypeIds }
        if (newLines.isEmpty()) return

        val enabledWallets = newLines.map { line ->
            val tokenQuery = TokenQuery(blockchainType, TokenType.XrpAsset(line.currency, line.issuer))
            EnabledWallet(
                tokenQueryId = tokenQuery.id,
                accountId = account.id,
                coinName = null,
                coinCode = XrpKit.displayCurrencyCode(line.currency),
                coinDecimals = null,
                coinImage = null,
            )
        }

        walletManager.saveEnabledWallets(enabledWallets)
    }

    private val TrustLine.isHeld: Boolean get() = balance > BigDecimal.ZERO
    private val TrustLine.tokenTypeId: String get() = TokenType.XrpAsset(currency, issuer).id
}
