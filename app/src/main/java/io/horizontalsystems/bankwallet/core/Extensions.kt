package io.horizontalsystems.bankwallet.core

import android.content.Intent
import android.os.Parcelable
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.uikit.SingleClickListener
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun <T> io.reactivex.Flowable<T>.subscribeAsync(disposables: CompositeDisposable, onNext: ((T) -> Unit)? = null, onError: ((Throwable) -> Unit)? = null, onComplete: (() -> Unit)? = null, onFinished: (() -> Unit)? = null): Disposable {
    val disposable = this
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .unsubscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .subscribeWith(object : io.reactivex.subscribers.DisposableSubscriber<T>() {
                override fun onNext(t: T) {
                    onNext?.invoke(t)
                }

                override fun onComplete() {
                    onComplete?.invoke()
                    onFinished?.invoke()
                }

                override fun onError(throwable: Throwable) {
                    onError?.invoke(throwable)
                    onFinished?.invoke()
                }
            })
    disposables.add(disposable)
    return disposable
}

fun <T> io.reactivex.subjects.PublishSubject<T>.subscribeAsync(disposables: CompositeDisposable, onNext: ((T) -> Unit)): Disposable {
    val disposable = this
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .unsubscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(onNext)

    disposables.add(disposable)
    return disposable
}

fun io.reactivex.Completable.subscribeAsync(disposables: CompositeDisposable, onComplete: (() -> Unit), onError: ((Throwable) -> Unit)): Disposable {
    val disposable = this
            .subscribeOn(io.reactivex.schedulers.Schedulers.io())
            .unsubscribeOn(io.reactivex.schedulers.Schedulers.io())
            .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            .subscribe(onComplete, onError)

    disposables.add(disposable)
    return disposable
}

fun <T> LiveData<T>.reObserve(owner: LifecycleOwner, observer: Observer<T>) {
    removeObserver(observer)
    observe(owner, observer)
}

//View

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

fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
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
