package cash.p.terminal.network.pirate.data.database.converter

import androidx.room.TypeConverter
import cash.p.terminal.network.pirate.domain.enity.ChangeNowAssociatedCoin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class CoinAssociationListConverter {
    @TypeConverter
    fun fromJson(value: String): List<ChangeNowAssociatedCoin> =
        Json.decodeFromString<List<ChangeNowAssociatedCoin>>(value)

    @TypeConverter
    fun toJson(list: List<ChangeNowAssociatedCoin>) = Json.encodeToString(list)
}