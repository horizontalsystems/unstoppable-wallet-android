package io.horizontalsystems.bankwallet.modules.walletconnect.list.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    var initialConnectionPrompted = false

    private val disposables = CompositeDisposable()
    var sectionItem by mutableStateOf<Section?>(null)
        private set

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

    fun onDelete(sessionsId: String) {
        service.kill(sessionsId)
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

    private fun sync(items: List<WalletConnectListService.Item>) {
        if (items.isEmpty()) {
            sectionItem = null
            return
        }
        val sessions = mutableListOf<WalletConnectListModule.SessionViewItem>()
        items.forEach { item ->
            val itemSessions = item.sessions.map { session ->
                WalletConnectListModule.SessionViewItem(
                    sessionId = session.remotePeerId,
                    title = session.remotePeerMeta.name,
                    subtitle = item.chain,
                    url = session.remotePeerMeta.url,
                    imageUrl = getSuitableIcon(session.remotePeerMeta.icons),
                )
            }
            sessions.addAll(itemSessions)
        }
        sectionItem = Section(WalletConnectListModule.Version.Version1, sessions)
    }

    private fun getSuitableIcon(imageUrls: List<String>): String? {
        return imageUrls.lastOrNull { it.endsWith("png", ignoreCase = true) }
    }

    override fun onCleared() {
        disposables.clear()
    }

}
