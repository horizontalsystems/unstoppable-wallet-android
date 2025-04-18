package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.Coin

val Coin.imageUrl: String
    get() = "https://p.cash/storage/coins/$uid/image.png"

val Coin.alternativeImageUrl: String?
    get() = image

val Coin.imagePlaceholder: Int
    get() = R.drawable.coin_placeholder
