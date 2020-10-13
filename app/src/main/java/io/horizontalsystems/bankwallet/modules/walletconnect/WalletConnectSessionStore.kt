package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.WCSessionStoreItem
import com.trustwallet.walletconnect.WCSessionStoreType
import com.trustwallet.walletconnect.models.WCPeerMeta
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject

class WalletConnectSessionStore {
    private val wcSessionStoreType = WCSessionStoreType(App.preferences)

    var storedItem: WCSessionStoreItem?
        get() = wcSessionStoreType.session
        set(value) {
            wcSessionStoreType.session = value

            storePeerMetaSubject.onNext(Unit)
        }

    val storedPeerMeta: WCPeerMeta?
        get() = storedItem?.remotePeerMeta

    private val storePeerMetaSubject = PublishSubject.create<Unit>()
    val storePeerMetaSignal = storePeerMetaSubject.toFlowable(BackpressureStrategy.LATEST)

}
