package io.horizontalsystems.marketkit.storage

import androidx.room.*
import io.horizontalsystems.marketkit.models.*

@Dao
interface TokenEntityDao {

    @Query("SELECT * FROM TokenEntity")
    fun getAll(): List<TokenEntity>

}
