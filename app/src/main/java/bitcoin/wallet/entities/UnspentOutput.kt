package bitcoin.wallet.entities

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.google.gson.annotations.SerializedName

@Entity(primaryKeys = ["transactionHash", "index"], tableName = "unspent_output")
class UnspentOutput(
        val value: Long,

        @SerializedName("tx_output_n")
        val index: Int,

        val confirmations: Long,

        @SerializedName("tx_hash")
        val transactionHash: String,

        val script: String
)

@Dao
interface UnspentOutputDao {

    @get:Query("SELECT * FROM unspent_output")
    val all: List<UnspentOutput>

    @Insert
    fun insertAll(vararg unspentOutputs: UnspentOutput)

    @Query("DELETE FROM unspent_output")
    fun truncate()

}
