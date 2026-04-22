package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.PoisonAddressDao
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.PoisonAddressType
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PoisonAddressManager(
    private val poisonAddressDao: PoisonAddressDao,
    private val contactsRepository: ContactsRepository,
) {
    private val _poisonDbChangedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val poisonDbChangedFlow: SharedFlow<Unit> = _poisonDbChangedFlow.asSharedFlow()

    companion object {
        private const val SIMILARITY_CHARS = 3
        private const val WHITELIST_MIN_SEND_COUNT = 3
    }

    @Suppress("ReturnCount")
    fun determinePoisonStatus(
        relevantAddress: String?,
        blockchainType: BlockchainType,
        accountId: String,
        isOutgoing: Boolean,
        isCreatedByWallet: Boolean,
    ): PoisonStatus {
        if (relevantAddress == null) return PoisonStatus.BLOCKCHAIN
        val normalized = relevantAddress.lowercase()
        val blockchainUid = blockchainType.uid

        if (isInAddressBook(normalized, blockchainType)) return PoisonStatus.ADDRESS_BOOK
        if (isOutgoing && isCreatedByWallet) return PoisonStatus.CREATED

        val existing = poisonAddressDao.get(normalized, blockchainUid, accountId)
        if (isWhitelisted(existing)) return PoisonStatus.BLOCKCHAIN
        if (existing?.type == PoisonAddressType.SCAM) return PoisonStatus.SUSPICIOUS

        val whitelisted = poisonAddressDao.getWhitelisted(blockchainUid, accountId, WHITELIST_MIN_SEND_COUNT)
        if (isSimilarToKnown(normalized, whitelisted)) {
            saveScamAddress(normalized, blockchainType, accountId)
            return PoisonStatus.SUSPICIOUS
        }

        return PoisonStatus.BLOCKCHAIN
    }

    fun getPoisonStatus(record: TransactionRecord): PoisonStatus {
        val blockchainType = record.source.blockchain.type
        val accountId = record.source.account.id
        val outgoing = isOutgoing(record)

        val relevantAddress = getRelevantAddress(record, outgoing)

        val isCreatedByWallet = when (record) {
            is EvmTransactionRecord -> outgoing && !record.foreignTransaction
            is TronTransactionRecord -> outgoing && !record.foreignTransaction
            else -> false
        }

        return determinePoisonStatus(
            relevantAddress = relevantAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = outgoing,
            isCreatedByWallet = isCreatedByWallet,
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

    fun isAddressSuspicious(address: String?, blockchainType: BlockchainType, accountId: String): Boolean {
        if (address == null) return false
        val normalized = address.lowercase()
        val blockchainUid = blockchainType.uid

        val existing = poisonAddressDao.get(normalized, blockchainUid, accountId)
        if (existing?.type == PoisonAddressType.SCAM) return true
        if (isWhitelisted(existing)) return false
        if (isInAddressBook(normalized, blockchainType)) return false

        val whitelisted = poisonAddressDao.getWhitelisted(blockchainUid, accountId, WHITELIST_MIN_SEND_COUNT)
        return isSimilarToKnown(normalized, whitelisted)
    }

    fun saveKnownAddress(address: String, blockchainType: BlockchainType, accountId: String) {
        poisonAddressDao.upsertKnownIncrementingCount(
            address = address.lowercase(),
            blockchainTypeUid = blockchainType.uid,
            accountId = accountId,
        )
        _poisonDbChangedFlow.tryEmit(Unit)
    }

    private fun saveScamAddress(normalizedAddress: String, blockchainType: BlockchainType, accountId: String) {
        poisonAddressDao.insertIgnore(
            PoisonAddress(normalizedAddress, blockchainType.uid, accountId, PoisonAddressType.SCAM)
        )
        _poisonDbChangedFlow.tryEmit(Unit)
    }

    private fun isWhitelisted(entry: PoisonAddress?): Boolean =
        entry?.type == PoisonAddressType.KNOWN && entry.sendCount >= WHITELIST_MIN_SEND_COUNT

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
