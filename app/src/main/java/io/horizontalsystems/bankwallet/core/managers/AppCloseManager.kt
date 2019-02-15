package io.horizontalsystems.bankwallet.core.managers

import io.reactivex.subjects.PublishSubject

class AppCloseManager{
    var appCloseSignal = PublishSubject.create<Unit>()
}
