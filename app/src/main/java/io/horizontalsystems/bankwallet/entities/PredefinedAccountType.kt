package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R

enum class PredefinedAccountType {
    MNEMONIC,
    EOS,
    BINANCE;

    val title
        get() = when (this) {
            MNEMONIC -> R.string.ManageKeys_12_words
            EOS -> R.string.ManageKeys_eos
            BINANCE -> R.string.ManageKeys_24_words
        }

    val coinCodes
        get() = when (this) {
            MNEMONIC -> "BTC, BCH, DASH, ETH, ERC-20"
            EOS -> "EOS"
            BINANCE -> "BNB"
        }
}
