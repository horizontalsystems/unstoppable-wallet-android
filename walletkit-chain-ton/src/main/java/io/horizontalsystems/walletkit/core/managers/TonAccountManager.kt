package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.EnabledWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.tonkit.models.Event
import io.horizontalsystems.tonkit.models.Jetton
import io.horizontalsystems.tonkit.models.JettonVerificationType
import io.horizontalsystems.tonkit.models.TagQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class TonAccountManager(
    private val accountManager: IAccountManager,
    private val walletManager: WalletManager,
    private val tonKitManager: TonKitManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
) {
    private val blockchainType: BlockchainType = BlockchainType.Ton
    private val logger = AppLogger("ton-account-manager")
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
        val newJettons = jettons
            .filter { !existingTokenTypeIds.contains(it.tokenType.id) }
            // Anyone can send dust to any address, and doing so is what puts a jetton in this
            // list, so its metadata is attacker-chosen. Blacklisted ones are the cases the
            // indexer has already judged, and adding them to the balance list under a name of
            // their own choosing is how a fake "USDT" or a scam claim prompt gets in front of
            // the user.
            .filter { it.verification != JettonVerificationType.BLACKLIST }

        if (newJettons.isEmpty()) return

        val enabledWallets = newJettons.map { jetton ->
            EnabledWallet(
                tokenQueryId = TokenQuery(BlockchainType.Ton, jetton.tokenType).id,
                accountId = account.id,
                coinName = jetton.name,
                coinCode = jetton.symbol,
                coinDecimals = jetton.decimals,
                // Deliberately not jetton.image: it is a URL the sender controls, and the balance
                // list would fetch it on every render, handing them the user's IP alongside the
                // TON address they sent the dust to. The EVM path stores no image either;
                // catalogued tokens still get theirs from marketKit.
                coinImage = null
            )
        }

        walletManager.saveEnabledWallets(enabledWallets)
    }
}
