package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.core.toHexString
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
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import java.math.BigInteger

class EvmSpamManager(
    localStorage: ILocalStorage,
    coinManager: ICoinManager,
    spamAddressStorage: SpamAddressStorage,
    marketKitWrapper: MarketKitWrapper,
    appConfigProvider: AppConfigProvider
) : BaseSpamManager(localStorage, coinManager, spamAddressStorage, marketKitWrapper, appConfigProvider) {

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

    override fun supports(blockchainType: BlockchainType): Boolean {
        return EvmBlockchainManager.blockchainTypes.contains(blockchainType)
    }

    private fun handleEvmKitStarted(evmKitManager: EvmKitManager?, blockchainType: BlockchainType) {
        val wrapper = evmKitManager?.evmKitWrapper ?: return
        val account = evmKitManager.currentAccount ?: return
        val spamConfig = spamConfig(blockchainType)

        coroutineScope.launch {
            sync(wrapper.evmKit, account, spamConfig)

            wrapper.evmKit.allTransactionsFlowable.asFlow().cancellable()
                .collect {
                    sync(wrapper.evmKit, account, spamConfig)
                }
        }
    }

    private fun sync(evmKit: EthereumKit, account: Account, spamConfig: SpamConfig) {
        val scanState = spamAddressStorage.getSpamScanState(spamConfig.blockchainType, account.id)
        val transactions = evmKit.getFullTransactionsAfterSingle(scanState?.lastTransactionHash).blockingGet()
        val lastHash = handle(transactions, evmKit.receiveAddress, spamConfig)
        lastHash?.let {
            spamAddressStorage.save(SpamScanState(spamConfig.blockchainType, account.id, it))
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

                        val minValue = coinsMap[transfer.contractAddress.hex]

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

}
