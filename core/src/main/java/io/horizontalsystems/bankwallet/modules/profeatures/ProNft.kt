package io.horizontalsystems.bankwallet.modules.profeatures

import java.math.BigInteger

enum class ProNft(val keyName: String, val tokenId: BigInteger) {
    YAK("yak", BigInteger("77929411300911548602579223184347481465604416464327802926072149574722519040001")),
    LEO("leo", BigInteger.valueOf(2)),
    HORSE("horse", BigInteger.valueOf(3))
}
