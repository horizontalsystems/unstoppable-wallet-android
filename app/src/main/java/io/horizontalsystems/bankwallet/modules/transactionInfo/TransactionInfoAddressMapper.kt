package io.horizontalsystems.bankwallet.modules.transactionInfo

object TransactionInfoAddressMapper {
    private val addressDescriptions = mapOf(
        "0x7a250d5630b4cf539739df2c5dacb4c659f2488d" to "Uniswap v.2",
        "0xe592427a0aece92de3edee1f18e0157c05861564" to "Uniswap v.3",
        "0x05ff2b0db69458a0750badebc4f9e13add608c7f" to "PancakeSwap",
        "0x10ed43c718714eb63d5aa57b78b54704e256024e" to "PancakeSwap v.2",
        "0x11111112542d85b3ef69ae05771c2dccff4faa26" to "1inch v3.0",
        "0x1111111254fb6c44bac0bed2854e76f90643097d" to "1inch v4.0"
    )

    fun map(address: String): String {
        return addressDescriptions[address.lowercase()] ?: address
    }

    fun title(address: String): String? {
        return addressDescriptions[address.lowercase()]
    }
}
