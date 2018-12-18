package io.horizontalsystems.bankwallet.entities

import com.google.gson.annotations.SerializedName

enum class Network (val rawValue: String) {
    @SerializedName("Main")
    MAIN("Main"),

    @SerializedName("Test")
    TEST("Test");

    val isMain: Boolean
        get() = this == MAIN

    val isTest: Boolean
        get() = this == TEST


    companion object {
        private val map = Network.values().associateBy(Network::rawValue)
        fun fromRawValue(rawValue: String) = map[rawValue] ?: MAIN
    }
}