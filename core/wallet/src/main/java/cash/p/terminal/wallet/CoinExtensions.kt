package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.Coin

val Coin.imageUrl: String
    get() {
        val coinURL = when (uid) {
            "wdash" -> "https://wdash.org/logo.png"
            else -> "https://pirate.place/storage/coins/$uid/image.png"
        }
        return coinURL
    }

val Coin.alternativeImageUrl: String?
    get() = image

val Coin.imagePlaceholder: Int
    get() = R.drawable.coin_placeholder
