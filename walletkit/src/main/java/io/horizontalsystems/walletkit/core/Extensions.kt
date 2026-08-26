package io.horizontalsystems.walletkit.core

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.sorting.FullCoinSortContext
import io.horizontalsystems.walletkit.core.sorting.SortCriterion
import io.horizontalsystems.walletkit.core.sorting.sortedByCriteria
import io.horizontalsystems.walletkit.modules.market.topplatforms.Platform
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.CoinInvestment
import io.horizontalsystems.marketkit.models.CoinTreasury
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.delay
import java.util.Optional

val <T> Optional<T>.orNull: T?
    get() = when {
        isPresent -> get()
        else -> null
    }

val Platform.iconUrl: String
    get() = "https://cdn.blocksdecoded.com/blockchain-icons/32px/$uid@3x.png"

val String.coinIconUrl: String
    get() = "https://cdn.blocksdecoded.com/coin-icons/32px/$this@3x.png"

val String.fiatIconUrl: String
    get()= "https://cdn.blocksdecoded.com/fiat-icons/$this@3x.png"

val CoinCategory.imageUrl: String
    get() = "https://cdn.blocksdecoded.com/category-icons/$uid@3x.png"

val CoinInvestment.Fund.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/fund-icons/$uid@3x.png"

val CoinTreasury.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/treasury-icons/$fundUid@3x.png"

fun List<FullCoin>.sortedByFilter(filter: String): List<FullCoin> {
    val base = listOf(SortCriterion.MarketCapRank, SortCriterion.NameAscending)
    val criteria = if (filter.isNotBlank()) listOf(SortCriterion.FilterRelevance) + base else base
    return sortedByCriteria(criteria, FullCoinSortContext(filter = filter))
}

val Language.displayNameStringRes: Int
    get() = when (this) {
        Language.English -> R.string.Language_English
        Language.Japanese -> R.string.Language_Japanese
        Language.Korean -> R.string.Language_Korean
        Language.Spanish -> R.string.Language_Spanish
        Language.SimplifiedChinese -> R.string.Language_SimplifiedChinese
        Language.TraditionalChinese -> R.string.Language_TraditionalChinese
        Language.French -> R.string.Language_French
        Language.Italian -> R.string.Language_Italian
        Language.Czech -> R.string.Language_Czech
        Language.Portuguese -> R.string.Language_Portuguese
    }

// String

fun String.hexToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

// ByteArray

fun ByteArray.toRawHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

fun ByteArray?.toHexString(): String {
    val rawHex = this?.toRawHexString() ?: return ""
    return "0x$rawHex"
}

fun String.stripHexPrefix(): String = removePrefix("0x")

fun String.hexStringToByteArray(): ByteArray {
    val cleaned = removePrefix("0x")
    require(cleaned.length % 2 == 0) { "Invalid hex string length" }
    return ByteArray(cleaned.length / 2) { i ->
        ((Character.digit(cleaned[i * 2], 16) shl 4) + Character.digit(cleaned[i * 2 + 1], 16)).toByte()
    }
}

fun String.hexStringToByteArrayOrNull(): ByteArray? = try {
    hexStringToByteArray()
} catch (e: Exception) {
    null
}

fun LockTimeInterval?.stringResId(): Int {
    return when (this) {
        LockTimeInterval.hour -> R.string.Send_LockTime_Hour
        LockTimeInterval.month -> R.string.Send_LockTime_Month
        LockTimeInterval.halfYear -> R.string.Send_LockTime_HalfYear
        LockTimeInterval.year -> R.string.Send_LockTime_Year
        null -> R.string.Send_LockTime_Off
    }
}

fun String.shorten(): String {
    val prefixes = listOf("0x", "bc", "bnb", "ltc", "bitcoincash:", "ecash:", "xpub", "ypub", "zpub")

    var prefix = ""
    for (p in prefixes) {
        if (this.startsWith(p)) {
            prefix = p
            break
        }
    }

    val withoutPrefix = this.removePrefix(prefix)

    val characters = 4
    return if (withoutPrefix.length > characters * 2)
        prefix + withoutPrefix.take(characters) + "..." + withoutPrefix.takeLast(characters)
    else
        this
}

suspend fun <T> retryWhen(
    times: Int,
    predicate: suspend (cause: Throwable) -> Boolean,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Throwable) {
            if (!predicate(e)) {
                throw e
            }
        }
        delay(1000)
    }
    return block()
}

val BlockchainType.blockTime : Long?
    get() = when (this) {
        BlockchainType.Ethereum -> 12
        BlockchainType.BinanceSmartChain,
        BlockchainType.Tron,
            -> 3

        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Fantom,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.RobinhoodChain,
            -> 2

        BlockchainType.Gnosis,
        BlockchainType.Stellar,
        BlockchainType.Ton,
            -> 5

        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
            -> 600

        BlockchainType.Dash,
        BlockchainType.Litecoin,
            -> 150

        BlockchainType.Zcash -> 75

        BlockchainType.Monero -> 120

        BlockchainType.Zano -> 60

        BlockchainType.ArbitrumOne -> 1

        else -> null
    }