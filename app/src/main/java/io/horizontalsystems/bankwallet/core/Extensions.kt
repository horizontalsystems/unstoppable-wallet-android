package io.horizontalsystems.bankwallet.core

import android.content.Intent
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.annotation.CheckResult
import coil.load
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.bankwallet.entities.label
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.views.SingleClickListener
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

val Coin.isCustom: Boolean get() = uid.startsWith(CustomToken.uidPrefix)

val Coin.iconUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/coin-icons/$uid@3x.png"

val CoinCategory.imageUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/category-icons/$uid@3x.png"

val CoinInvestment.Fund.logoUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/fund-icons/$uid@3x.png"

val CoinTreasury.logoUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/treasury-icons/$fundUid@3x.png"

val Auditor.logoUrl: String
    get() = "https://markets.nyc3.digitaloceanspaces.com/auditor-icons/$name@3x.png"

val FullCoin.iconPlaceholder: Int
    get() = if (platforms.size == 1) {
        platforms.first().coinType.iconPlaceholder
    } else {
        R.drawable.coin_placeholder
    }

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

val CoinType.iconPlaceholder: Int
    get() = when (this) {
        is CoinType.Erc20 -> R.drawable.erc20
        is CoinType.Bep2 -> R.drawable.bep2
        is CoinType.Bep20 -> R.drawable.bep20
        else -> R.drawable.coin_placeholder
    }

val FullCoin.typeLabel: String?
    get() = if (platforms.size == 1) {
        platforms.first().coinType.label
    } else {
        null
    }

val CoinType.blockchainLogo: Int
    get() = when (this) {
        CoinType.Bitcoin -> R.drawable.logo_bitcoin_24
        CoinType.Ethereum -> R.drawable.logo_ethereum_24
        CoinType.BitcoinCash -> R.drawable.logo_bitcoincash_24
        CoinType.Dash -> R.drawable.logo_dash_24
        CoinType.BinanceSmartChain -> R.drawable.logo_binancesmartchain_24
        is CoinType.Bep2 -> R.drawable.logo_bep2_24
        CoinType.Litecoin -> R.drawable.logo_litecoin_24
        else -> R.drawable.coin_placeholder
    }

// ImageView

fun ImageView.setRemoteImage(url: String, placeholder: Int = R.drawable.ic_placeholder) {
    load(url) {
        error(placeholder)
    }
}

fun ImageView.setImage(imageSource: ImageSource) {
    when (imageSource) {
        is ImageSource.Local -> setImageResource(imageSource.resId)
        is ImageSource.Remote -> setRemoteImage(imageSource.url, imageSource.placeholder)
    }
}

// View

fun View.setOnSingleClickListener(l: ((v: View) -> Unit)) {
    this.setOnClickListener(
        object : SingleClickListener() {
            override fun onSingleClick(v: View) {
                l.invoke(v)
            }
        })
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
