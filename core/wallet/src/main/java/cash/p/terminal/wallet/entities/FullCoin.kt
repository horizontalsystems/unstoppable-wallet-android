package cash.p.terminal.wallet.entities

import cash.p.terminal.wallet.Token

data class FullCoin(
    val coin: Coin,
    val tokens: List<Token>
) {

    override fun toString(): String {
        return "FullCoin [ \n$coin, \n${tokens.joinToString(separator = ",\n")} \n]"
    }

}
