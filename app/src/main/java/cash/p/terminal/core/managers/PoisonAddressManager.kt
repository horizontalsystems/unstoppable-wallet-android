package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.PoisonAddressDao
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.PoisonAddressType
import cash.p.terminal.entities.transactionrecords.PendingTransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class PoisonAddressManager(
    private val poisonAddressDao: PoisonAddressDao,
    private val contactsRepository: ContactsRepository,
    private val locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository,
    private val dispatcherProvider: DispatcherProvider,
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
        if (relevantAddress == null) {
            return if (isOutgoing && isCreatedByWallet) PoisonStatus.CREATED else PoisonStatus.BLOCKCHAIN
        }
        val normalized = relevantAddress.lowercase()
        val blockchainUid = blockchainType.uid

        if (isInAddressBook(normalized, blockchainType)) return PoisonStatus.ADDRESS_BOOK
        if (isOutgoing && isCreatedByWallet) return PoisonStatus.CREATED

        val existing = poisonAddressDao.get(normalized, blockchainUid, accountId)
        if (isWhitelisted(existing)) return PoisonStatus.BLOCKCHAIN
        if (existing?.type == PoisonAddressType.SCAM) return PoisonStatus.SUSPICIOUS

        if (isSimilarToKnown(normalized, getKnownAddresses(blockchainType, accountId))) {
            saveScamAddress(normalized, blockchainType, accountId)
            return PoisonStatus.SUSPICIOUS
        }

        return PoisonStatus.BLOCKCHAIN
    }

    suspend fun getPoisonStatus(record: TransactionRecord): PoisonStatus = withContext(dispatcherProvider.io) {
        val blockchainType = record.source.blockchain.type
        val account = record.source.account
        val accountId = account.id
        val outgoing = isOutgoing(record)

        val relevantAddress = getRelevantAddress(record, outgoing)

        determinePoisonStatus(
            relevantAddress = relevantAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = outgoing,
            isCreatedByWallet = isCreatedByWallet(record, outgoing, account.isWatchAccount),
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

        return isSimilarToKnown(normalized, getKnownAddresses(blockchainType, accountId))
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

    private suspend fun isCreatedByWallet(
        record: TransactionRecord,
        outgoing: Boolean,
        isWatchAccount: Boolean,
    ): Boolean {
        if (isWatchAccount) return false
        if (!outgoing) return false

        if (record is PendingTransactionRecord) return true

        return locallyCreatedTransactionRepository.isCreated(record)
    }

    private fun isOutgoing(record: TransactionRecord): Boolean {
        if (record is PendingTransactionRecord) return true

        return when (record.transactionRecordType) {
            TransactionRecordType.BITCOIN_OUTGOING,
            TransactionRecordType.SOLANA_OUTGOING,
            TransactionRecordType.MONERO_OUTGOING,
            TransactionRecordType.STELLAR_OUTGOING -> true

            TransactionRecordType.TON -> record.to != null && record.from == null

            TransactionRecordType.EVM_APPROVE,
            TransactionRecordType.EVM_CONTRACT_CALL,
            TransactionRecordType.EVM_CONTRACT_CREATION,
            TransactionRecordType.EVM_OUTGOING,
            TransactionRecordType.EVM_SWAP,
            TransactionRecordType.EVM_UNKNOWN_SWAP -> true

            TransactionRecordType.TRON_APPROVE,
            TransactionRecordType.TRON_CONTRACT_CALL,
            TransactionRecordType.TRON_OUTGOING -> true

            TransactionRecordType.UNKNOWN,
            TransactionRecordType.SOLANA_INCOMING,
            TransactionRecordType.SOLANA_UNKNOWN,
            TransactionRecordType.BITCOIN_INCOMING,
            TransactionRecordType.EVM,
            TransactionRecordType.EVM_INCOMING,
            TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL,
            TransactionRecordType.TRON,
            TransactionRecordType.TRON_EXTERNAL_CONTRACT_CALL,
            TransactionRecordType.TRON_INCOMING,
            TransactionRecordType.MONERO_INCOMING,
            TransactionRecordType.STELLAR_INCOMING -> false
        }
    }

    private fun isSimilarToKnown(
        normalizedAddress: String,
        knownAddresses: List<String>
    ): Boolean {
        if (normalizedAddress.length < SIMILARITY_CHARS * 2) return false
        val prefix = normalizedAddress.take(SIMILARITY_CHARS)
        val suffix = normalizedAddress.takeLast(SIMILARITY_CHARS)
        return knownAddresses.any { known ->
            known != normalizedAddress &&
                    known.take(SIMILARITY_CHARS) == prefix &&
                    known.takeLast(SIMILARITY_CHARS) == suffix
        }
    }

    private fun getKnownAddresses(
        blockchainType: BlockchainType,
        accountId: String,
    ): List<String> {
        val whitelisted = poisonAddressDao
            .getWhitelisted(blockchainType.uid, accountId, WHITELIST_MIN_SEND_COUNT)
            .map { it.address }
        val contactAddresses = contactsRepository
            .getContactsFiltered(blockchainType = blockchainType)
            .flatMap { contact ->
                contact.addresses
                    .filter { it.blockchain.type == blockchainType }
                    .map { it.address.lowercase() }
            }
        return whitelisted + contactAddresses
    }
}
