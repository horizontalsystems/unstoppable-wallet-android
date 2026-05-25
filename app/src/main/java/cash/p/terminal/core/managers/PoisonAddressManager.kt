package cash.p.terminal.core.managers

import cash.p.terminal.core.isEvm
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
        private const val SIMILARITY_MIN_SEND_COUNT = 1
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

        if (isSimilarToKnown(normalized, getKnownAddresses(blockchainType, accountId), blockchainType)) {
            saveScamAddress(normalized, blockchainType, accountId)
            return PoisonStatus.SUSPICIOUS
        }

        return PoisonStatus.BLOCKCHAIN
    }

    fun getPoisonStatus(record: TransactionRecord): PoisonStatus {
        val blockchainType = record.source.blockchain.type
        val account = record.source.account
        val accountId = account.id
        val outgoing = isOutgoing(record)

        val relevantAddress = getRelevantAddress(record, outgoing)

        // Watch accounts have no signing key — they observe the chain, they cannot
        // author transactions. Any tx visible at a watched address came from outside,
        // even if `from` equals the watched address.
        val isCreatedByWallet = !account.isWatchAccount && when (record) {
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

        return isSimilarToKnown(normalized, getKnownAddresses(blockchainType, accountId), blockchainType)
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
        knownAddresses: List<String>,
        blockchainType: BlockchainType,
    ): Boolean {
        val prefixChars = prefixCharsToMatch(blockchainType)
        if (normalizedAddress.length < prefixChars + SIMILARITY_CHARS) return false
        val prefix = normalizedAddress.take(prefixChars)
        val suffix = normalizedAddress.takeLast(SIMILARITY_CHARS)
        return knownAddresses.any { known ->
            known != normalizedAddress &&
                    known.take(prefixChars) == prefix &&
                    known.takeLast(SIMILARITY_CHARS) == suffix
        }
    }

    /**
     * Number of leading characters that must match for a prefix to be considered similar.
     *
     * Some chains begin every address with a constant, auto-generated service prefix
     * (e.g. EVM `0x`, Tron `T`). Matching that prefix is effectively free for an attacker,
     * so it must not count as meaningful entropy. Instead of stripping the prefix we extend
     * the comparison window by its length, keeping [SIMILARITY_CHARS] significant characters
     * after it.
     */
    private fun prefixCharsToMatch(blockchainType: BlockchainType): Int =
        SIMILARITY_CHARS + constantPrefixLength(blockchainType)

    /**
     * Length of the constant service prefix for chains in scope (EVM and account-based chains).
     *
     * Only the prefix *length* is used, not the exact characters — the length is stable across
     * a chain's address sub-formats (TON bounceable/non-bounceable `EQ`/`UQ`/`kQ`/`0Q` are all
     * 2 chars, Monero standard/subaddress `4`/`8` are both 1). This is a per-chain
     * approximation; UTXO chains are intentionally out of scope and keep the default window.
     */
    private fun constantPrefixLength(blockchainType: BlockchainType): Int = when {
        blockchainType.isEvm -> 2                       // "0x"
        blockchainType == BlockchainType.Tron -> 1      // "T"
        blockchainType == BlockchainType.Stellar -> 1   // "G" (and muxed "M")
        blockchainType == BlockchainType.Ton -> 2       // "EQ" / "UQ" and other 2-char forms
        blockchainType == BlockchainType.Monero -> 1    // "4" / "8"
        else -> 0
    }

    private fun getKnownAddresses(
        blockchainType: BlockchainType,
        accountId: String,
    ): List<String> {
        val knownAddresses = poisonAddressDao
            .getWhitelisted(blockchainType.uid, accountId, SIMILARITY_MIN_SEND_COUNT)
            .map { it.address }
        val contactAddresses = contactsRepository
            .getContactsFiltered(blockchainType = blockchainType)
            .flatMap { contact ->
                contact.addresses
                    .filter { it.blockchain.type == blockchainType }
                    .map { it.address.lowercase() }
            }
        return knownAddresses + contactAddresses
    }
}
