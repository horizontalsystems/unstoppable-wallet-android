package cash.p.terminal.core.managers

import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.entities.EnabledWallet
import cash.p.terminal.wallet.entities.TokenQuery
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.logger.AppLogger
import io.horizontalsystems.stellarkit.TagQuery
import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.let

class StellarAccountManager(
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val stellarKitManager: StellarKitManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
) {
    private val blockchainType: BlockchainType = BlockchainType.Stellar
    private val logger = AppLogger("stellar-account-manager")
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val singleDispatcherCoroutineScope = CoroutineScope(singleDispatcher)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var transactionSubscriptionJob: Job? = null

    fun start() {
        singleDispatcherCoroutineScope.launch {
            stellarKitManager.kitStartedFlow.collect { started ->
                handleStarted(started)
            }
        }
    }

    private suspend fun handleStarted(started: Boolean) {
        try {
            if (started) {
                subscribeToTransactions()
            } else {
                stop()
            }
        } catch (exception: Exception) {
            logger.warning("error", exception)
        }
    }

    private fun stop() {
        transactionSubscriptionJob?.cancel()
    }

    private suspend fun subscribeToTransactions() {
        val stellarKitWrapper = stellarKitManager.stellarKitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        transactionSubscriptionJob = coroutineScope.launch {
            stellarKitWrapper.stellarKit.operationFlow(TagQuery(null, null, null))
                .collect { (operations, initial) ->
                    handle(operations, account, stellarKitWrapper, initial)
                }
        }
    }

    private fun handle(
        operations: List<Operation>,
        account: Account,
        stellarKitWrapper: StellarKitWrapper,
        initial: Boolean,
    ) {
        val shouldAutoEnableTokens = tokenAutoEnableManager.isAutoEnabled(account, blockchainType)

        if (initial && account.origin == AccountOrigin.Restored && !account.isWatchAccount && !shouldAutoEnableTokens) {
            return
        }

        val assets = mutableSetOf<StellarAsset.Asset>()

        operations.forEach { operation ->
            operation.payment?.let { payment ->
                val stellarAsset = payment.asset
                if (stellarAsset is StellarAsset.Asset) {
                    assets.add(stellarAsset)
                }
            }
        }

        handle(assets, account)
    }

    private fun handle(assets: Set<StellarAsset.Asset>, account: Account) {
        if (assets.isEmpty()) return

        val existingWallets = walletManager.activeWallets
        val existingTokenTypeIds = existingWallets.map { it.token.type.id }
        val newAssets = assets.filter { !existingTokenTypeIds.contains(it.tokenType.id) }

        if (newAssets.isEmpty()) return

        val enabledWallets = newAssets.map { asset ->
            val tokenQuery = TokenQuery(BlockchainType.Stellar, asset.tokenType)

            EnabledWallet(
                tokenQueryId = tokenQuery.id,
                accountId = account.id,
                coinName = null,
                coinCode = asset.code,
                coinDecimals = null,
                coinImage = null
            )
        }

        walletManager.saveEnabledWallets(enabledWallets)
    }
}
