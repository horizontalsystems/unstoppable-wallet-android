package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.Coin

val Coin.imageUrl: String
    get() {
        var pirate: String = "piratecash"
        var cosa: String = "cosanta"
        var wdash: String = "wdash"
        val coinURL = when (uid) {
            pirate -> "https://pirate.cash/logo.png"
            cosa -> "https://cosanta.net/logo.png"
            wdash -> "https://wdash.org/logo.png"
            else -> "https://cdn.blocksdecoded.com/coin-icons/32px/$uid@3x.png"
        }
        return coinURL
    }

val Coin.alternativeImageUrl: String?
    get() = image

val Coin.imagePlaceholder: Int
    get() = R.drawable.coin_placeholder
