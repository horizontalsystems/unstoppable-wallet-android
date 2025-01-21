package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nftkit.events.Eip1155TransferEventInstance
import io.horizontalsystems.nftkit.events.Eip721TransferEventInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal
import java.math.BigInteger

class SpamManager(
    private val localStorage: ILocalStorage,
    private val coinManager: ICoinManager,
    private val spamAddressStorage: SpamAddressStorage,
    marketKitWrapper: MarketKitWrapper,
    appConfigProvider: AppConfigProvider
) {
    private val coinValueLimits = appConfigProvider.spamCoinValueLimits
    private val coins = marketKitWrapper.fullCoins(coinValueLimits.map { it.key })
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var transactionSubscriptionJob: Job? = null

    private val stableCoinCodes = listOf("USDT", "USDC", "DAI", "BUSD", "EURS")
    private val negligibleValue = BigDecimal("0.01")

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun isSpam(
        incomingEvents: List<TransferEvent>,
        outgoingEvents: List<TransferEvent>
    ): Boolean {
        val allEvents = incomingEvents + outgoingEvents
        return allEvents.all { spamEvent(it) }
    }

    private fun spamEvent(event: TransferEvent): Boolean {
        return when (val eventValue = event.value) {
            is TransactionValue.CoinValue -> {
                spamValue(eventValue.coinCode, eventValue.value)
            }

            is TransactionValue.NftValue -> {
                eventValue.value <= BigDecimal.ZERO
            }

            else -> true
        }
    }

    private fun spamValue(coinCode: String, value: BigDecimal): Boolean {
        return if (stableCoinCodes.contains(coinCode)) {
            value < negligibleValue
        } else {
            value <= BigDecimal.ZERO
        }
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    private fun scanSpamAddresses(fullTransaction: FullTransaction, userAddress: Address, spamConfig: SpamConfig): List<Address> {
        val transaction = fullTransaction.transaction
        val baseCoinValue = spamConfig.baseCoinValue
        val coinsMap = spamConfig.coinsMap
        val blockchainType = spamConfig.blockchainType

        when (val decoration = fullTransaction.decoration) {
            is IncomingDecoration -> {
                val from = transaction.from

                if (from != null && decoration.value <= baseCoinValue) {
                    return listOf(from)
                }
            }

            is UnknownTransactionDecoration -> {
                if (transaction.from == userAddress) {
                    return emptyList()
                } else if (transaction.to != userAddress) {
                    val spamAddresses = mutableListOf<Address>()

                    val internalTransactions = decoration.internalTransactions.filter { it.to == userAddress }
                    val totalIncomingValue = internalTransactions.sumOf { it.value }
                    val addresses = internalTransactions.map { it.from }

                    if (totalIncomingValue <= baseCoinValue) {
                        spamAddresses.addAll(addresses)
                    }

                    val eip20Transfers = decoration.eventInstances.filterIsInstance<TransferEventInstance>()
                    for (transfer in eip20Transfers) {
                        val query = TokenQuery(blockchainType, TokenType.Eip20(transfer.contractAddress.hex))

                        val minValue = coinsMap[transfer.contractAddress]

                        val isSpam = if (minValue != null) {
                            transfer.value <= minValue
                        } else if (coinManager.getToken(query) != null) {
                            transfer.value == BigInteger.ZERO
                        } else {
                            true
                        }

                        if (isSpam) {
                            val counterpartyAddress = if (transfer.from == userAddress) transfer.to else transfer.from
                            spamAddresses.add(counterpartyAddress)
                        } else {
                            return emptyList()
                        }
                    }

                    val eip721Transfers = decoration.eventInstances.filterIsInstance<Eip721TransferEventInstance>()
                    val eip1155Transfers = decoration.eventInstances.filterIsInstance<Eip1155TransferEventInstance>()

                    return if (eip721Transfers.isNotEmpty() || eip1155Transfers.isNotEmpty())
                        emptyList()
                    else
                        spamAddresses
                }
            }

            else -> Unit
        }

        return emptyList()
    }

    private fun scaleUp(value: Double, decimals: Int): BigInteger {
        return BigDecimal(value).movePointRight(decimals).toBigInteger()
    }

    private fun spamConfig(blockchainType: BlockchainType): SpamConfig {
        val tokens = coins.map { coin -> coin.tokens.filter { it.blockchainType == blockchainType } }.flatten()
        var baseCoinValue = BigInteger.ZERO

        val coinsMap = mutableMapOf<Address, BigInteger>()
        for (token in tokens) {
            val value = coinValueLimits[token.coin.uid] ?: continue

            when (val tokenType = token.type) {
                is TokenType.Eip20 -> {
                    try {
                        val address = Address(tokenType.address)
                        coinsMap[address] = scaleUp(value, token.decimals)
                    } catch (err: Throwable) {
                        Unit
                    }
                }

                is TokenType.Native -> {
                    baseCoinValue = scaleUp(value, token.decimals)
                }

                else -> Unit
            }
        }
        return SpamConfig(baseCoinValue, coinsMap, blockchainType)
    }

    fun subscribeToKitStart(evmKitManager: EvmKitManager, blockchainType: BlockchainType) {
        coroutineScope.launch {
            evmKitManager.kitStartedObservable
                .asFlow()
                .collect { started ->
                    if (started) {
                        handleEvmKitStarted(evmKitManager, blockchainType)
                    }
                }
        }
    }

    fun find(address: String): SpamAddress? {
        return spamAddressStorage.findByAddress(address)
    }

    fun isSpam(transactionHash: ByteArray): Boolean {
        return spamAddressStorage.isSpam(transactionHash)
    }

    private fun handleEvmKitStarted(evmKitManager: EvmKitManager?, blockchainType: BlockchainType) {
        val evmKitWrapper = evmKitManager?.evmKitWrapper ?: return
        val currentAccount = evmKitManager.currentAccount ?: return

        val spamConfig = spamConfig(blockchainType)

        transactionSubscriptionJob = coroutineScope.launch {
            evmKitWrapper.evmKit.allTransactionsFlowable.asFlow().cancellable()
                .collect { (fullTransactions, _) ->
                    handle(fullTransactions, evmKitWrapper.evmKit.receiveAddress, spamConfig)
                }
        }

        sync(evmKitWrapper.evmKit, currentAccount, spamConfig)
    }

    private fun sync(evmKit: EthereumKit, account: Account, spamConfig: SpamConfig) {
        val spamScanState = spamAddressStorage.getSpamScanState(spamConfig.blockchainType, account.id)

        val fullTransactions = evmKit.getFullTransactionsAfterSingle(spamScanState?.lastTransactionHash).blockingGet()
        val lastTransactionHash = handle(fullTransactions, evmKit.receiveAddress, spamConfig)

        lastTransactionHash?.let {
            spamAddressStorage.save(SpamScanState(spamConfig.blockchainType, account.id, lastTransactionHash))
        }
    }

    private fun handle(fullTransactions: List<FullTransaction>, userAddress: Address, spamConfig: SpamConfig): ByteArray? {
        val spamAddresses = mutableListOf<SpamAddress>()

        fullTransactions.forEach { fullTransaction ->
            val addresses = scanSpamAddresses(fullTransaction, userAddress, spamConfig).map { address ->
                SpamAddress(
                    transactionHash = fullTransaction.transaction.hash,
                    address = address.hex.uppercase(),
                    domain = null,
                    blockchainType = spamConfig.blockchainType
                )
            }
            spamAddresses.addAll(addresses)
        }

        try {
            spamAddressStorage.save(spamAddresses)
        } catch (_: Throwable) {
        }

        val sortedTransactions = fullTransactions.map { it.transaction }.sortedWith(
            compareBy<Transaction> { it.timestamp }
                .thenBy { it.transactionIndex }
                .thenBy { it.hash.toHexString() }
        )

        return sortedTransactions.lastOrNull()?.hash
    }
}

data class SpamConfig(
    val baseCoinValue: BigInteger,
    val coinsMap: Map<Address, BigInteger>,
    val blockchainType: BlockchainType,
)
