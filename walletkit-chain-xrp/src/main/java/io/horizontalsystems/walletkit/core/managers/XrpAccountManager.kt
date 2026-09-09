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
        val kit = kitManager.kitWrapper?.kit ?: return
        val account = accountManager.activeAccount ?: return

        subscriptionJob = coroutineScope.launch {
            kit.trustLinesFlow.collect { lines ->
                handle(lines, account)
            }
        }
    }

    private fun handle(lines: List<TrustLine>, account: Account) {
        val shouldAutoEnable = tokenAutoEnableManager.isAutoEnabled(account, blockchainType)
        if (account.origin == AccountOrigin.Restored && !account.isWatchAccount && !shouldAutoEnable) return
        if (!tokenAutoEnableManager.autoEnableTokensOnReceive) return

        val held = lines.filter { it.balance > BigDecimal.ZERO }
        if (held.isEmpty()) return

        val existingTokenTypeIds = walletManager.activeWallets.map { it.token.type.id }
        val newLines = held.filter { TokenType.XrpAsset(it.currency, it.issuer).id !in existingTokenTypeIds }
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
}
