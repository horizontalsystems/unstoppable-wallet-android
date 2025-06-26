package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.decoration.NativeTransactionDecoration
import io.horizontalsystems.tronkit.decoration.UnknownTransactionDecoration
import io.horizontalsystems.tronkit.decoration.trc20.Trc20TransferEvent
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.FullTransaction
import io.horizontalsystems.tronkit.models.Transaction
import io.horizontalsystems.tronkit.models.TransferContract
import kotlinx.coroutines.launch
import java.math.BigInteger

class TronSpamManager(
    localStorage: ILocalStorage,
    coinManager: ICoinManager,
    spamAddressStorage: SpamAddressStorage,
    marketKitWrapper: MarketKitWrapper,
    appConfigProvider: AppConfigProvider
) : BaseSpamManager(localStorage, coinManager, spamAddressStorage, marketKitWrapper, appConfigProvider) {

    private val spamConfig by lazy { createSpamConfig(BlockchainType.Tron) }

    fun subscribeToKitStart(kitManager: TronKitManager) {
        coroutineScope.launch {
            kitManager.kitStartedFlow
                .collect { started ->
                    if (started) {
                        handleEvmKitStarted(kitManager)
                    }
                }
        }
    }

    override fun supports(blockchainType: BlockchainType): Boolean {
        return blockchainType == BlockchainType.Tron
    }

    fun isSpam(fullTransaction: FullTransaction, userAddress: Address): Boolean {
        return scanSpamAddresses(fullTransaction, userAddress, spamConfig).isNotEmpty()
    }

    private fun handleEvmKitStarted(kitManager: TronKitManager?) {
        val wrapper = kitManager?.tronKitWrapper ?: return
        val account = kitManager.currentAccount ?: return

        coroutineScope.launch {
            sync(wrapper.tronKit, account, spamConfig)

            wrapper.tronKit.transactionsFlow
                .collect {
                    sync(wrapper.tronKit, account, spamConfig)
                }
        }
    }

    private suspend fun sync(tronKit: TronKit, account: Account, spamConfig: SpamConfig) {
        val scanState = spamAddressStorage.getSpamScanState(spamConfig.blockchainType, account.id)
        val transactions = tronKit.getFullTransactionsAfter(scanState?.lastTransactionHash)
        if (transactions.isEmpty()) return

        val lastHash = handle(transactions, tronKit.address, spamConfig)
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
                    address = address.base58,
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
                .thenBy { it.hash.toHexString() }
        )

        return sortedTransactions.lastOrNull()?.hash
    }

    private fun scanSpamAddresses(fullTransaction: FullTransaction, userAddress: Address, spamConfig: SpamConfig): List<Address> {
        val baseCoinValue = spamConfig.baseCoinValue
        val coinsMap = spamConfig.coinsMap
        val blockchainType = spamConfig.blockchainType
        when (val decoration = fullTransaction.decoration) {
            is NativeTransactionDecoration -> {
                val contract = decoration.contract
                if (contract is TransferContract && contract.ownerAddress != userAddress && contract.amount < baseCoinValue) {
                    return listOf(contract.ownerAddress)
                }
            }

            is UnknownTransactionDecoration -> {
                if (decoration.fromAddress == userAddress) {
                    return emptyList()
                } else if (decoration.toAddress != userAddress) {
                    val spamAddresses = mutableListOf<Address>()

                    val internalTransactions = decoration.internalTransactions.filter { it.to == userAddress }
                    val totalIncomingValue = internalTransactions.sumOf { it.value }
                    val addresses = internalTransactions.map { it.from }

                    if (totalIncomingValue <= baseCoinValue) {
                        spamAddresses.addAll(addresses)
                    }

                    val eip20Transfers = decoration.events.filterIsInstance<Trc20TransferEvent>()
                    for (transfer in eip20Transfers) {
                        val query = TokenQuery(blockchainType, TokenType.Eip20(transfer.contractAddress.base58))

                        val minValue = coinsMap[transfer.contractAddress.base58]

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

                    return spamAddresses
                }
            }

            else -> Unit
        }

        return emptyList()
    }

}
