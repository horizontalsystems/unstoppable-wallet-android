package org.grouvi.wallet.core

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
