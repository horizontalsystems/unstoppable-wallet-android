package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class WC2SessionManager {

    private val TAG = "WC2SessionManager"

    private val sessionsSubject = PublishSubject.create<List<String>>()
    val sessionsObservable: Flowable<List<String>>
        get() = sessionsSubject.toFlowable(BackpressureStrategy.BUFFER)

}
