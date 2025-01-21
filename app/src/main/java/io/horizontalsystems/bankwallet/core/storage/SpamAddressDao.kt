package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.marketkit.models.BlockchainType

@Dao
interface SpamAddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(spamAddress: SpamAddress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(spamAddresses: List<SpamAddress>)

    @Query("SELECT * FROM SpamAddress WHERE address = :address LIMIT 1")
    fun getByAddress(address: String): SpamAddress?

    @Query("SELECT * FROM SpamAddress WHERE transactionHash = :hash LIMIT 1")
    fun getByTransaction(hash: ByteArray): SpamAddress?

    @Query("SELECT * FROM SpamAddress")
    fun getAll(): List<SpamAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(spamAddress: SpamScanState)

    @Query("SELECT * FROM SpamScanState WHERE blockchainType = :blockchainType AND accountId = :accountId")
    fun getSpamScanState(blockchainType: BlockchainType, accountId: String): SpamScanState?

}
