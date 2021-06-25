package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import java.util.*

object TransactionInfoAddressMapper {
    private val addressDescriptions = mapOf(
            "0x7a250d5630b4cf539739df2c5dacb4c659f2488d" to "Uniswap v.2",
            "0xe592427a0aece92de3edee1f18e0157c05861564" to "Uniswap v.3",
            "0x05ff2b0db69458a0750badebc4f9e13add608c7f" to "PancakeSwap",
            "0x11111112542d85b3ef69ae05771c2dccff4faa26" to "1inch"
    )

    fun map(address: String): String {
        return addressDescriptions[address.toLowerCase(Locale.ENGLISH)] ?: address
    }
}
