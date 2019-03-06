package io.horizontalsystems.bankwallet.core

import android.app.Activity
import android.graphics.Color
import android.view.View
import io.horizontalsystems.bankwallet.ui.view.SingleClickListener
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

//View

fun View.setOnSingleClickListener(l: ((v: View) -> Unit)) {
    this.setOnClickListener(
            object : SingleClickListener() {
                override fun onSingleClick(v: View) {
                    l.invoke(v)
                }
            })
}

fun Activity.setTransparentStatusBar() {
    window.statusBarColor = Color.TRANSPARENT
}
