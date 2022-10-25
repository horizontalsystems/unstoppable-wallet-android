package io.horizontalsystems.bankwallet.core

import android.content.Intent
import android.os.Parcelable
import android.widget.ImageView
import androidx.annotation.CheckResult
import coil.load
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.market.topplatforms.Platform
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

val <T> Optional<T>.orNull: T?
    get() = when {
        isPresent -> get()
        else -> null
    }

val Platform.iconUrl: String
    get() = "https://cdn.blocksdecoded.com/blockchain-icons/32px/$uid@3x.png"

val Coin.iconUrl: String
    get() = "https://cdn.blocksdecoded.com/coin-icons/32px/$uid@3x.png"

val CoinCategory.imageUrl: String
    get() = "https://cdn.blocksdecoded.com/category-icons/$uid@3x.png"

val CoinInvestment.Fund.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/fund-icons/$uid@3x.png"

val CoinTreasury.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/treasury-icons/$fundUid@3x.png"

val Auditor.logoUrl: String
    get() = "https://cdn.blocksdecoded.com/auditor-icons/$name@3x.png"

fun List<FullCoin>.sortedByFilter(filter: String, enabled: (FullCoin) -> Boolean): List<FullCoin> {
    var comparator: Comparator<FullCoin> = compareByDescending {
        enabled.invoke(it)
    }
    if (filter.isNotBlank()) {
        val lowercasedFilter = filter.lowercase()
        comparator = comparator
            .thenByDescending {
                it.coin.code.lowercase() == lowercasedFilter
            }.thenByDescending {
                it.coin.code.lowercase().startsWith(lowercasedFilter)
            }.thenByDescending {
                it.coin.name.lowercase().startsWith(lowercasedFilter)
            }
    }
    comparator = comparator.thenBy {
        it.coin.marketCapRank ?: Int.MAX_VALUE
    }
    comparator = comparator.thenBy {
        it.coin.name.lowercase(Locale.ENGLISH)
    }

    return sortedWith(comparator)
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

// ImageView

fun ImageView.setRemoteImage(url: String, placeholder: Int? = R.drawable.ic_placeholder) {
    load(url) {
        if (placeholder != null) {
            error(placeholder)
        }
    }
}

fun ImageView.setImage(imageSource: ImageSource) {
    when (imageSource) {
        is ImageSource.Local -> setImageResource(imageSource.resId)
        is ImageSource.Remote -> setRemoteImage(imageSource.url, imageSource.placeholder)
    }
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

// Intent & Parcelable Enum
fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
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

@CheckResult
fun <T> Observable<T>.subscribeIO(onNext: (t: T) -> Unit): Disposable {
    return this
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(onNext)
}

@CheckResult
fun <T> Observable<T>.subscribeIO(onSuccess: (t: T) -> Unit, onError: (e: Throwable) -> Unit): Disposable {
    return this
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(onSuccess, onError)
}

@CheckResult
fun <T> Flowable<T>.subscribeIO(onNext: (t: T) -> Unit): Disposable {
    return this
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(onNext)
}

@CheckResult
fun <T> Single<T>.subscribeIO(onSuccess: (t: T) -> Unit, onError: (e: Throwable) -> Unit): Disposable {
    return this
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(onSuccess, onError)
}

@CheckResult
fun <T> Single<T>.subscribeIO(onSuccess: (t: T) -> Unit): Disposable {
    return this
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(onSuccess)
}

fun String.shorten(): String {
    val prefixes = listOf("0x", "bc", "bnb", "ltc", "bitcoincash:")

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