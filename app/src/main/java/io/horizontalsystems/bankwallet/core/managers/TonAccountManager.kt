package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.tonkit.models.Event
import io.horizontalsystems.tonkit.models.Jetton
import io.horizontalsystems.tonkit.models.TagQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class TonAccountManager(
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val tonKitManager: TonKitManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
) {
    private val blockchainType: BlockchainType = BlockchainType.Ton
    private val logger = AppLogger("evm-account-manager")
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val singleDispatcherCoroutineScope = CoroutineScope(singleDispatcher)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var transactionSubscriptionJob: Job? = null

    fun start() {
        singleDispatcherCoroutineScope.launch {
            tonKitManager.kitStartedFlow.collect { started ->
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
        val tonKitWrapper = tonKitManager.tonKitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        transactionSubscriptionJob = coroutineScope.launch {
            tonKitWrapper.tonKit.eventFlow(TagQuery(null, null, null, null))
                .collect { (events, initial) ->
                    handle(events, account, tonKitWrapper, initial)
                }
        }
    }

    private fun handle(
        events: List<Event>,
        account: Account,
        tonKitWrapper: TonKitWrapper,
        initial: Boolean,
    ) {
        val shouldAutoEnableTokens = tokenAutoEnableManager.isAutoEnabled(account, blockchainType)

        if (initial && account.origin == AccountOrigin.Restored && !account.isWatchAccount && !shouldAutoEnableTokens) {
            return
        }

        val address = tonKitWrapper.tonKit.receiveAddress

        val jettons = mutableSetOf<Jetton>()

        events.forEach { event ->
            event.actions.forEach { action ->
                action.jettonTransfer?.let {
                    if (it.recipient?.address == address) {
                        jettons.add(it.jetton)
                    }
                }
                action.jettonMint?.let {
                    if (it.recipient.address == address) {
                        jettons.add(it.jetton)
                    }
                }
                action.jettonSwap?.let {
                    it.jettonMasterIn?.let {
                        jettons.add(it)
                    }
                }
            }
        }

        handle(jettons, account)
    }

    private fun handle(jettons: Set<Jetton>, account: Account) {
        if (jettons.isEmpty()) return

        val existingWallets = walletManager.activeWallets
        val existingTokenTypeIds = existingWallets.map { it.token.type.id }
        val newJettons = jettons.filter { !existingTokenTypeIds.contains(it.tokenType.id) }

        if (newJettons.isEmpty()) return

        val enabledWallets = newJettons.map { jetton ->
            EnabledWallet(
                tokenQueryId = TokenQuery(BlockchainType.Ton, jetton.tokenType).id,
                accountId = account.id,
                coinName = jetton.name,
                coinCode = jetton.symbol,
                coinDecimals = jetton.decimals,
                coinImage = jetton.image
            )
        }

        walletManager.saveEnabledWallets(enabledWallets)
    }
}
