package cash.p.terminal.core.managers

import cash.p.terminal.core.isEvm
import cash.p.terminal.core.storage.PoisonAddressDao
import cash.p.terminal.core.tryOrNull
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.PoisonAddressType
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal

class PoisonAddressManager(
    private val poisonAddressDao: PoisonAddressDao,
    private val contactsRepository: ContactsRepository,
    private val marketKit: MarketKitWrapper,
) {
    private val _poisonDbChangedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val poisonDbChangedFlow: SharedFlow<Unit> = _poisonDbChangedFlow.asSharedFlow()

    companion object {
        private const val SIMILARITY_CHARS = 3
        // False positives are currently worse than losing this signal entirely.
        private const val SUSPICIOUS_DETECTION_ENABLED = false
    }

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    fun determinePoisonStatus(
        relevantAddress: String?,
        blockchainType: BlockchainType,
        isOutgoing: Boolean,
        isCreatedByWallet: Boolean,
        amount: BigDecimal? = null,
        coinCode: String? = null,
        contractAddress: String? = null,
    ): PoisonStatus {
        if (relevantAddress == null) return PoisonStatus.BLOCKCHAIN
        val normalized = relevantAddress.lowercase()
        val blockchainUid = blockchainType.uid

        if (isInAddressBook(normalized, blockchainType)) return PoisonStatus.ADDRESS_BOOK
        if (isOutgoing && isCreatedByWallet) return PoisonStatus.CREATED

        val existing = poisonAddressDao.get(normalized, blockchainUid)
        if (existing?.type == PoisonAddressType.KNOWN) return PoisonStatus.BLOCKCHAIN
        if (existing?.type == PoisonAddressType.SCAM && SUSPICIOUS_DETECTION_ENABLED) return PoisonStatus.SUSPICIOUS

        if (isOutgoing && amount != null && amount.compareTo(BigDecimal.ZERO) == 0
            && SUSPICIOUS_DETECTION_ENABLED) {
            saveScamAddress(normalized, blockchainType)
            return PoisonStatus.SUSPICIOUS
        }

        if (coinCode != null) {
            val upperCode = coinCode.uppercase()
            if (upperCode == "USDT" || upperCode == "USDC") {
                val isEvmCompatible = blockchainType.isEvm || blockchainType == BlockchainType.Tron
                if (contractAddress != null && !isKnownStablecoinContract(contractAddress, blockchainType)
                    && SUSPICIOUS_DETECTION_ENABLED) {
                    // Known contract that's not in our DB — fake
                    saveScamAddress(normalized, blockchainType)
                    return PoisonStatus.SUSPICIOUS
                }
                if (contractAddress == null && isEvmCompatible
                    && SUSPICIOUS_DETECTION_ENABLED) {
                    // On EVM chains, a legitimate stablecoin always has a known contract.
                    // Null contract means unknown token (TokenValue/RawValue) — likely fake.
                    // On non-EVM chains (TON jettons, Stellar assets), null contract is normal.
                    saveScamAddress(normalized, blockchainType)
                    return PoisonStatus.SUSPICIOUS
                }
            }
        }

        val knownAddresses = poisonAddressDao.getAllByType(PoisonAddressType.KNOWN, blockchainUid)
        if (isSimilarToKnown(normalized, knownAddresses)
            && SUSPICIOUS_DETECTION_ENABLED) {
            saveScamAddress(normalized, blockchainType)
            return PoisonStatus.SUSPICIOUS
        }

        return PoisonStatus.BLOCKCHAIN
    }

    fun getPoisonStatus(record: TransactionRecord): PoisonStatus {
        val blockchainType = record.source.blockchain.type
        val outgoing = isOutgoing(record)

        // For EXTERNAL_CONTRACT_CALL, addresses are in events, not on record.from/to
        val relevantAddress = getRelevantAddress(record, outgoing)

        val isCreatedByWallet = when (record) {
            is EvmTransactionRecord -> outgoing && !record.foreignTransaction
            is TronTransactionRecord -> outgoing && !record.foreignTransaction
            else -> false
        }

        val mainValue = record.mainValue
        val amount = mainValue?.decimalValue
        val coinCode = mainValue?.coinCode
        // Extract contract address from CoinValue or infer from events for unknown tokens
        val contractAddress = extractContractAddress(mainValue, record)

        return determinePoisonStatus(
            relevantAddress = relevantAddress,
            blockchainType = blockchainType,
            isOutgoing = outgoing,
            isCreatedByWallet = isCreatedByWallet,
            amount = amount,
            coinCode = coinCode,
            contractAddress = contractAddress,
        )
    }

    private fun getRelevantAddress(record: TransactionRecord, outgoing: Boolean): String? {
        // Standard case: use from/to on record
        val standard = if (outgoing) record.to?.firstOrNull() else record.from
        if (standard != null) return standard

        // For EXTERNAL_CONTRACT_CALL/events-based records, extract from events.
        // When outgoing, prefer outgoing events (destination); when incoming, prefer incoming events (sender).
        if (record is EvmTransactionRecord) {
            return if (outgoing)
                record.exchangeAddress ?:
                record.outgoingEvents?.firstOrNull()?.address
                ?: record.incomingEvents?.firstOrNull()?.address
            else
                record.incomingEvents?.firstOrNull()?.address
                    ?: record.outgoingEvents?.firstOrNull()?.address
        }
        if (record is TronTransactionRecord) {
            return if (outgoing)
                record.outgoingEvents?.firstOrNull()?.address
                    ?: record.incomingEvents?.firstOrNull()?.address
            else
                record.incomingEvents?.firstOrNull()?.address
                    ?: record.outgoingEvents?.firstOrNull()?.address
        }
        return null
    }

    private fun extractContractAddress(
        mainValue: TransactionValue?,
        record: TransactionRecord
    ): String? {
        // Try CoinValue path first (known tokens)
        (mainValue as? TransactionValue.CoinValue)
            ?.token?.type
            ?.let { it as? TokenType.Eip20 }
            ?.address
            ?.let { return it }

        // For unknown tokens (TokenValue/RawValue), try to get contract from events
        if (record is EvmTransactionRecord) {
            val event = record.incomingEvents?.firstOrNull() ?: record.outgoingEvents?.firstOrNull()
            val eventValue = event?.value
            if (eventValue is TransactionValue.CoinValue) {
                (eventValue.token.type as? TokenType.Eip20)?.address?.let { return it }
            }
        }
        return null
    }

    fun isAddressSuspicious(address: String?, blockchainType: BlockchainType): Boolean {
        if (!SUSPICIOUS_DETECTION_ENABLED) return false
        if (address == null) return false
        val normalized = address.lowercase()
        val blockchainUid = blockchainType.uid

        val existing = poisonAddressDao.get(normalized, blockchainUid)
        if (existing?.type == PoisonAddressType.SCAM) return true
        if (existing?.type == PoisonAddressType.KNOWN) return false
        if (isInAddressBook(normalized, blockchainType)) return false

        val knownAddresses = poisonAddressDao.getAllByType(PoisonAddressType.KNOWN, blockchainUid)
        return isSimilarToKnown(normalized, knownAddresses)
    }

    fun saveKnownAddress(address: String, blockchainType: BlockchainType) {
        poisonAddressDao.insert(
            PoisonAddress(address.lowercase(), blockchainType.uid, PoisonAddressType.KNOWN)
        )
        _poisonDbChangedFlow.tryEmit(Unit)
    }

    private fun saveScamAddress(normalizedAddress: String, blockchainType: BlockchainType) {
        poisonAddressDao.insertIgnore(
            PoisonAddress(normalizedAddress, blockchainType.uid, PoisonAddressType.SCAM)
        )
        _poisonDbChangedFlow.tryEmit(Unit)
    }

    private fun isKnownStablecoinContract(
        contractAddress: String,
        blockchainType: BlockchainType,
    ): Boolean {
        val query = TokenQuery(blockchainType, TokenType.Eip20(contractAddress))
        return tryOrNull { marketKit.token(query) } != null
    }

    private fun isInAddressBook(
        normalizedAddress: String,
        blockchainType: BlockchainType
    ): Boolean {
        return contactsRepository.getContactsFiltered(
            blockchainType = blockchainType,
            addressQuery = normalizedAddress
        ).isNotEmpty()
    }

    private fun isOutgoing(record: TransactionRecord): Boolean {
        return when (record.transactionRecordType) {
            TransactionRecordType.BITCOIN_OUTGOING,
            TransactionRecordType.EVM_OUTGOING,
            TransactionRecordType.EVM_SWAP,
            TransactionRecordType.EVM_UNKNOWN_SWAP,
            TransactionRecordType.TRON_OUTGOING,
            TransactionRecordType.SOLANA_OUTGOING,
            TransactionRecordType.MONERO_OUTGOING,
            TransactionRecordType.STELLAR_OUTGOING -> true

            TransactionRecordType.TON -> record.to != null && record.from == null
            else -> false
        }
    }

    private fun isSimilarToKnown(
        normalizedAddress: String,
        knownAddresses: List<PoisonAddress>
    ): Boolean {
        if (normalizedAddress.length < SIMILARITY_CHARS * 2) return false
        val prefix = normalizedAddress.take(SIMILARITY_CHARS)
        val suffix = normalizedAddress.takeLast(SIMILARITY_CHARS)
        return knownAddresses.any { known ->
            known.address != normalizedAddress &&
                    known.address.take(SIMILARITY_CHARS) == prefix &&
                    known.address.takeLast(SIMILARITY_CHARS) == suffix
        }
    }
}
