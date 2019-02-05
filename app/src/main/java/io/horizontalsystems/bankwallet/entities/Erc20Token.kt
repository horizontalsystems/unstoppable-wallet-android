package io.horizontalsystems.bankwallet.entities

import com.google.gson.annotations.SerializedName

data class Erc20Token(
        @SerializedName("code") val code: String,
        @SerializedName("name") val name: String,
        @SerializedName("contract") val contract: String,
        @SerializedName("decimal") val decimal: Int
)
