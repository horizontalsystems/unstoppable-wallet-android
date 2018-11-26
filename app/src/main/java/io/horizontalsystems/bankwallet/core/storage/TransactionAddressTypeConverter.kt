package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.entities.TransactionAddress

class TransactionAddressTypeConverter {

    private val gson = Gson()

    @TypeConverter
    fun toJsonString(addresses: List<TransactionAddress>): String =
            gson.toJson(addresses)

    @TypeConverter
    fun toTransactionAddressList(data: String?): List<TransactionAddress> =
            if (data == null || data.isBlank()) {
                emptyList()
            } else {
                val listType = object : TypeToken<List<TransactionAddress>>() {}.type
                gson.fromJson(data, listType)
            }

}
