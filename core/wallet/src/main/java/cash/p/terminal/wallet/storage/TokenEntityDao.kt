package cash.p.terminal.wallet.storage

import androidx.room.*
import cash.p.terminal.wallet.models.TokenEntity

@Dao
interface TokenEntityDao {

    @Query("SELECT * FROM TokenEntity")
    fun getAll(): List<TokenEntity>

}
