package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.storage.EvmAccountStateDao
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.EvmAccountState
import io.horizontalsystems.erc20kit.core.DataProvider
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigInteger
import java.util.concurrent.Executors

class EvmAccountManager(
    private val blockchainType: BlockchainType,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
    private val evmKitManager: EvmKitManager,
    private val evmAccountStateDao: EvmAccountStateDao
) {
    private val logger = AppLogger("evm-account-manager")
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val singleDispatcherCoroutineScope = CoroutineScope(singleDispatcher)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var transactionSubscriptionJob: Job? = null

    init {
        singleDispatcherCoroutineScope.launch {
            evmKitManager.kitStartedObservable
                .asFlow()
                .collect { started ->
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
        val evmKitWrapper = evmKitManager.evmKitWrapper ?: return
        val account = accountManager.activeAccount ?: return

        transactionSubscriptionJob = coroutineScope.launch {
            evmKitWrapper.evmKit.allTransactionsFlowable.asFlow().cancellable()
                .collect { (fullTransactions, initial) ->
                    handle(fullTransactions, account, evmKitWrapper, initial)
                }
        }
    }

    private fun handle(fullTransactions: List<FullTransaction>, account: Account, evmKitWrapper: EvmKitWrapper, initial: Boolean) {
        val restored = evmAccountStateDao.get(account.id, evmKitManager.chain.id)?.restored ?: false

        if (initial && account.origin == AccountOrigin.Restored && !account.isWatchAccount && !restored) {
            return
        }

        val address = evmKitWrapper.evmKit.receiveAddress

        val foundTokens = mutableSetOf<FoundToken>()
        val suspiciousTokenTypes = mutableSetOf<TokenType>()

        for (fullTransaction in fullTransactions) {
            when (val decoration = fullTransaction.decoration) {
                is IncomingDecoration -> {
                    foundTokens.add(FoundToken(TokenType.Native))
                }

                is SwapDecoration -> {
                    val tokenOut = decoration.tokenOut
                    if (tokenOut is SwapDecoration.Token.Eip20Coin) {
                        foundTokens.add(FoundToken(TokenType.Eip20(tokenOut.address.hex), tokenOut.tokenInfo))
                    }
                }

                is OneInchSwapDecoration -> {
                    val tokenOut = decoration.tokenOut
                    if (tokenOut is OneInchDecoration.Token.Eip20Coin) {
                        foundTokens.add(FoundToken(TokenType.Eip20(tokenOut.address.hex), tokenOut.tokenInfo))
                    }
                }

                is OneInchUnoswapDecoration -> {
                    val tokenOut = decoration.tokenOut
                    if (tokenOut is OneInchDecoration.Token.Eip20Coin) {
                        foundTokens.add(FoundToken(TokenType.Eip20(tokenOut.address.hex), tokenOut.tokenInfo))
                    }
                }

                is UnknownTransactionDecoration -> {
                    if (decoration.internalTransactions.any { it.to == address }) {
                        foundTokens.add(FoundToken(TokenType.Native))
                    }

                    for (eventInstance in decoration.eventInstances) {
                        if (eventInstance !is TransferEventInstance) continue

                        if (eventInstance.to == address) {
                            val tokenType = TokenType.Eip20(eventInstance.contractAddress.hex)

                            if (decoration.fromAddress == address) {
                                foundTokens.add(FoundToken(tokenType, eventInstance.tokenInfo))
                            } else {
                                suspiciousTokenTypes.add(tokenType)
                            }
                        }
                    }
                }
            }
        }

        handle(
            foundTokens = foundTokens.toList(),
            suspiciousTokenTypes = suspiciousTokenTypes.minus(foundTokens.map { it.tokenType }.toSet()).toList(),
            account = account,
            evmKit = evmKitWrapper.evmKit
        )
    }

    private fun handle(
        foundTokens: List<FoundToken>,
        suspiciousTokenTypes: List<TokenType>,
        account: Account,
        evmKit: EthereumKit
    ) {
        if (foundTokens.isEmpty() && suspiciousTokenTypes.isEmpty()) return

        /*Log.e("AAA", "FOUND TOKEN TYPES: ${foundTokens.size}: \n ${
            foundTokens.joinToString(separator = "\n") { "${it.tokenType.id} --- ${it.tokenInfo?.tokenName} --- ${it.tokenInfo?.tokenSymbol} --- ${it.tokenInfo?.tokenDecimal}" }
        }")

        Log.e(
            "AAA",
            "SUSPICIOUS TOKEN TYPES: ${suspiciousTokenTypes.size}: \n ${suspiciousTokenTypes.joinToString(separator = "\n") { "${it.id} " }}"
        )*/

        try {
            val queries = (foundTokens.map { it.tokenType } + suspiciousTokenTypes).map { TokenQuery(blockchainType, it) }
            val tokens = marketKit.tokens(queries)
            val tokenInfos = mutableListOf<TokenInfo>()

            foundTokens.forEach { foundToken ->
                val token = tokens.firstOrNull { it.type == foundToken.tokenType }
                if (token != null) {
                    tokenInfos.add(
                        TokenInfo(
                            type = foundToken.tokenType,
                            coinName = token.coin.name,
                            coinCode = token.coin.code,
                            tokenDecimals = token.decimals
                        )
                    )
                } else if (foundToken.tokenInfo != null) {
                    tokenInfos.add(
                        TokenInfo(
                            type = foundToken.tokenType,
                            coinName = foundToken.tokenInfo.tokenName,
                            coinCode = foundToken.tokenInfo.tokenSymbol,
                            tokenDecimals = foundToken.tokenInfo.tokenDecimal
                        )
                    )
                }
            }

            suspiciousTokenTypes.forEach { tokenType ->
                val token = tokens.firstOrNull { it.type == tokenType }
                if (token != null) {
                    tokenInfos.add(
                        TokenInfo(
                            type = tokenType,
                            coinName = token.coin.name,
                            coinCode = token.coin.code,
                            tokenDecimals = token.decimals
                        )
                    )
                }
            }
            coroutineScope.launch {
                handle(tokenInfos, account, evmKit)
            }
        } catch (ex: Exception) {

        }
    }

    private suspend fun handle(tokenInfos: List<TokenInfo>, account: Account, evmKit: EthereumKit) = withContext(Dispatchers.IO) {
//        Log.e("AAA", "handle tokens ${tokenInfos.size} \n ${tokenInfos.joinToString(separator = " ") { it.type.id }}")

        val existingWallets = walletManager.activeWallets
        val existingTokenTypeIds = existingWallets.map { it.token.type.id }
        val newTokenInfos = tokenInfos.filter { !existingTokenTypeIds.contains(it.type.id) }

//        Log.e("AAA", "New Tokens: ${newTokenInfos.size}")

        if (newTokenInfos.isEmpty()) return@withContext

        val userAddress = evmKit.receiveAddress
        val dataProvider = DataProvider(evmKit)

        val requests = newTokenInfos.map { tokenInfo ->
            val contractAddress = (tokenInfo.type as? TokenType.Eip20)?.let {
                try {
                    Address(it.address)
                } catch (ex: Exception) {
                    null
                }
            }

            async {
                if (contractAddress != null) {
                    val balance = try {
                        dataProvider.getBalance(contractAddress, userAddress).await()
                    } catch (error: Throwable) {
                        null
                    }

                    if (balance == null || balance > BigInteger.ZERO) {
                        tokenInfo
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }

        val enabledWallets = requests.awaitAll().filterNotNull().map { tokenInfo ->
            EnabledWallet(
                tokenQueryId = TokenQuery(blockchainType, tokenInfo.type).id,
                coinSettingsId = "",
                accountId = account.id,
                coinName = tokenInfo.coinName,
                coinCode = tokenInfo.coinCode,
                coinDecimals = tokenInfo.tokenDecimals

            )
        }

        if (enabledWallets.isNotEmpty()) {
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }

    fun markAutoEnable(account: Account) {
        evmAccountStateDao.insert(EvmAccountState(account.id, evmKitManager.chain.id, 0, true))
    }

    data class TokenInfo(
        val type: TokenType,
        val coinName: String,
        val coinCode: String,
        val tokenDecimals: Int
    )

    data class FoundToken(
        val tokenType: TokenType,
        val tokenInfo: io.horizontalsystems.erc20kit.events.TokenInfo? = null
    ) {
        override fun equals(other: Any?): Boolean {
            return other is FoundToken && tokenType.id == other.tokenType.id
        }

        override fun hashCode(): Int {
            return tokenType.id.hashCode()
        }
    }

}
