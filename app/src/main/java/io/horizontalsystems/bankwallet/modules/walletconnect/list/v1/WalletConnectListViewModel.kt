package io.horizontalsystems.bankwallet.modules.walletconnect.list.v1

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule.Section
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1SessionKillManager
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.net.UnknownHostException

class WalletConnectListViewModel(
    private val service: WalletConnectListService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    val sectionLiveData = MutableLiveData<Section?>()
    val killingSessionInProcessLiveEvent = SingleLiveEvent<Unit>()
    val killingSessionCompletedLiveEvent = SingleLiveEvent<Unit>()
    val killingSessionFailedLiveEvent = SingleLiveEvent<String>()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        if (service.items.isNotEmpty()) {
            sync(service.items)
        }

        service.sessionKillingStateObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }
    }

    private fun sync(state: WC1SessionKillManager.State) {
        when (state) {
            is WC1SessionKillManager.State.Failed -> {
                val errorMessage = if (state.error is UnknownHostException)
                    Translator.getString(R.string.Hud_Text_NoInternet)
                else
                    state.error.message ?: state.error::class.java.simpleName

                killingSessionFailedLiveEvent.postValue(errorMessage)
            }
            WC1SessionKillManager.State.Killed -> {
                killingSessionCompletedLiveEvent.postValue(Unit)
            }
            WC1SessionKillManager.State.NotConnected,
            WC1SessionKillManager.State.Processing -> {
                killingSessionInProcessLiveEvent.postValue(Unit)
            }
        }
    }

//    fun onClickDelete(sessionViewItem: WalletConnectViewItem.Session) {
//        service.kill(sessionViewItem.session)
//    }

    private fun sync(items: List<WalletConnectListService.Item>) {
        if (items.isEmpty()){
            sectionLiveData.postValue(null)
            return
        }
        val sessions = mutableListOf<WalletConnectListModule.SessionViewItem>()
        items.forEach { item ->
            val itemSessions = item.sessions.map { session ->
                WalletConnectListModule.SessionViewItem(
                    sessionId = session.remotePeerId,
                    title = session.remotePeerMeta.name,
                    subtitle = item.chain.title,
                    url = session.remotePeerMeta.url,
                    imageUrl = getSuitableIcon(session.remotePeerMeta.icons),
                )
            }
            sessions.addAll(itemSessions)
        }
        sectionLiveData.postValue(Section(WalletConnectListModule.Version.Version1, sessions))
    }

    private fun getSuitableIcon(imageUrls: List<String>): String? {
        return imageUrls.lastOrNull { it.endsWith("png", ignoreCase = true) }
    }

    override fun onCleared() {
        disposables.clear()
    }

}
