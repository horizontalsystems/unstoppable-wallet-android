package bitcoin.wallet.entities

import android.arch.persistence.room.*

@Entity(tableName = "exchange_rate")
class ExchangeRate(
        @PrimaryKey
        val code: String,

        val value: Double
)

@Dao
interface ExchangeRateDao {

    @get:Query("SELECT * FROM exchange_rate")
    val all: List<ExchangeRate>

    @Insert
    fun insertAll(vararg exchangeRates: ExchangeRate)

    @Query("DELETE FROM exchange_rate")
    fun truncate()

}
