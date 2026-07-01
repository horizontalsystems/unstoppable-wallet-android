package io.horizontalsystems.bankwallet.entities

import java.math.BigDecimal

sealed class AddressUriParserResult{
    data class Data(val addressData: AddressData): AddressUriParserResult()
    object NoUri: AddressUriParserResult()
    object WrongUri: AddressUriParserResult()
}

data class AddressData(
        val address: String,
        val version: String? = null,
        val amount: BigDecimal? = null,
        val label: String? = null,
        val message: String? = null,
        val parameters: MutableMap<String, String>? = null) {

    val uriPaymentAddress: String
        get() {
            val uriAddress = address
            version?.let {
                uriAddress.plus(";version=$version")
            }
            amount?.let {
                uriAddress.plus("?amount=$it")
            }
            label?.let {
                uriAddress.plus("?label=$label")
            }
            message?.let {
                uriAddress.plus("?message=$message")
            }
            parameters?.let {
                for ((name, value) in it) {
                    uriAddress.plus("?$name=$value")
                }
            }

            return uriAddress
        }
}
