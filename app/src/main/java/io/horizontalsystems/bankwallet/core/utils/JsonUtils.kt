package io.horizontalsystems.bankwallet.core.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.core.AccountType

object JsonUtils {

    val gson: Gson

    init {
        val adapter = RuntimeTypeAdapterFactory
                .of(AccountType::class.java)
                .registerSubtype(AccountType.Mnemonic::class.java)
                .registerSubtype(AccountType.MasterKey::class.java)
                .registerSubtype(AccountType.HDMasterKey::class.java)
                .registerSubtype(AccountType.Eos::class.java)

        gson = GsonBuilder().registerTypeAdapterFactory(adapter).create()
    }
}
