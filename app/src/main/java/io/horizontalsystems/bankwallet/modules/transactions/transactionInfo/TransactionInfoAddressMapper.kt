package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import java.util.*

object TransactionInfoAddressMapper {
    private val addressDescriptions = mapOf("0x7a250d5630b4cf539739df2c5dacb4c659f2488d" to "Uniswap v.2")

    fun map(address: String): String {
        return addressDescriptions[address.toLowerCase(Locale.ENGLISH)] ?: address
    }
}
