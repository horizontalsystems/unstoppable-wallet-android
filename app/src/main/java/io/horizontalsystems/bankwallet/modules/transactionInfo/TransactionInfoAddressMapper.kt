package io.horizontalsystems.bankwallet.modules.transactionInfo

object TransactionInfoAddressMapper {
    private val addressDescriptions = mapOf(
        // Ethereum Mainnet
        "0x7a250d5630b4cf539739df2c5dacb4c659f2488d" to "Uniswap v.2",
        "0xe592427a0aece92de3edee1f18e0157c05861564" to "Uniswap v.3",
        "0x68b3465833fb72A70ecDF485E0e4C7bD8665Fc45" to "Uniswap v.3",
        "0x11111112542d85b3ef69ae05771c2dccff4faa26" to "1Inch V3",
        "0x881d40237659c251811cec9c364ef91dc08d300c" to "Metamask: Swap Router",
        "0xc2edad668740f1aa35e4d8f227fb8e17dca888cd" to "SushiSwap",
        "0xd9e1ce17f2641f24ae83637ab66a2cca9c378b9f" to "SushiSwap",
        "0x8798249c2e607446efb7ad49ec89dd1865ff4272" to "SushiSwap",
        "0x1111111254fb6c44bac0bed2854e76f90643097d" to "1Inch V4",

        // Binance Smart Chain
        "0x05ff2b0db69458a0750badebc4f9e13add608c7f" to "PancakeSwap",
        "0x10ed43c718714eb63d5aa57b78b54704e256024e" to "PancakeSwap v.2",
        "0xf84e3809971798bd372aecdc03ae977759a619ab" to "Bunny Compensation Pool",
        "0xcadc8cb26c8c7cb46500e61171b5f27e9bd7889d" to "Pancake Bunny: Bunny Pool",

        // Polygon
        "0xa5e0829caced8ffdd4de3c43696c57f7d7a678ff" to "QuickSwap",

        // Optimism
        "0x1111111254760f7ab3f16433eea9304126dcd199" to "1Inch V4",
    )

    fun map(address: String): String {
        return addressDescriptions[address.lowercase()] ?: address
    }

    fun title(address: String): String? {
        return addressDescriptions[address.lowercase()]
    }
}
