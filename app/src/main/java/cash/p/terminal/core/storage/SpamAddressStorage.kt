package cash.p.terminal.core.storage

import cash.p.terminal.entities.SpamAddress
import cash.p.terminal.entities.SpamScanState
import io.horizontalsystems.core.entities.BlockchainType


class SpamAddressStorage(
    private val spamAddressDao: SpamAddressDao
) {

    fun isSpam(hash: ByteArray): Boolean =
        spamAddressDao.getByTransaction(hash) != null

    fun findByAddress(address: String): SpamAddress? =
        spamAddressDao.getByAddress(address)

    fun save(spamAddresses: List<SpamAddress>) {
        spamAddressDao.insertAll(spamAddresses)
    }

    fun save(spamScanState: SpamScanState) {
        spamAddressDao.insert(spamScanState)
    }

    fun getSpamScanState(blockchainType: BlockchainType, accountUid: String): SpamScanState? =
        spamAddressDao.getSpamScanState(blockchainType, accountUid)
}
