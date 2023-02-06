package io.horizontalsystems.marketkit.models

data class FullCoin(
    val coin: Coin,
    val tokens: List<Token>
) {

    override fun toString(): String {
        return "FullCoin [ \n$coin, \n${tokens.joinToString(separator = ",\n")} \n]"
    }

}
