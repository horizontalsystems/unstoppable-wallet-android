package io.horizontalsystems.bankwallet.core

import android.content.Intent
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.views.SingleClickListener
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


fun Coin.getIconUrl() : String {
    return "https://markets.nyc3.digitaloceanspaces.com/coin-icons/ios/$uid@3x.png"
}

fun CoinCategory.imageUrl(): String {
    return "https://markets.nyc3.digitaloceanspaces.com/category-icons/ios/$uid@3x.png"
}

fun FullCoin.getPlaceholder(): Int {
    return if(platforms.size == 1) platforms[0].coinType.getPlaceholder() else R.drawable.place_holder
}

fun CoinType.getPlaceholder(): Int {
    return when (this) {
        is CoinType.Erc20 -> R.drawable.erc20
        is CoinType.Bep2 -> R.drawable.bep2
        is CoinType.Bep20 -> R.drawable.bep20
        else -> R.drawable.place_holder
    }
}

//View

fun ImageView.setCoinImage(coinType: CoinType) {
    setImageDrawable(AppLayoutHelper.getCoinDrawable(context, coinType))
}

fun ImageView.setCoinImage(coinUid: String) {
    // TODO: set image using coinUid
    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.place_holder))
}

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
