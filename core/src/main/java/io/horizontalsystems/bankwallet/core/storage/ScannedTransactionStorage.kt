package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.marketkit.models.BlockchainType

class ScannedTransactionStorage(
    private val dao: ScannedTransactionDao
) {
    fun getScannedTransaction(hash: ByteArray): ScannedTransaction? =
        dao.getByHash(hash)

    fun getScannedTransactions(hashes: List<ByteArray>): List<ScannedTransaction> =
        dao.getByHashes(hashes)

    fun isSpam(hash: ByteArray): Boolean =
        dao.getByHash(hash)?.isSpam == true

    fun findSpamByAddress(address: String): ScannedTransaction? =
        dao.getSpamByAddress(address)

    fun save(scannedTransaction: ScannedTransaction) {
        dao.insert(scannedTransaction)
    }

    fun save(scannedTransactions: List<ScannedTransaction>) {
        dao.insertAll(scannedTransactions)
    }

    fun save(spamScanState: SpamScanState) {
        dao.insert(spamScanState)
    }

    fun getSpamScanState(blockchainType: BlockchainType, accountUid: String): SpamScanState? =
        dao.getSpamScanState(blockchainType, accountUid)
}