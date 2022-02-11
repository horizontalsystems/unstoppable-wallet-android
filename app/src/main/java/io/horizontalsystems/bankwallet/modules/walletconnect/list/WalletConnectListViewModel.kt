package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionKillManager
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule.Section
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable
import java.net.UnknownHostException

class WalletConnectListViewModel(
    private val service: WalletConnectListService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    val sectionsLiveData = MutableLiveData<List<Section>>()
    val startNewConnectionEvent = SingleLiveEvent<Unit>()
    val killingSessionInProcessLiveEvent = SingleLiveEvent<Unit>()
    val killingSessionCompletedLiveEvent = SingleLiveEvent<Unit>()
    val killingSessionFailedLiveEvent = SingleLiveEvent<String>()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        if (service.items.isEmpty()) {
            startNewConnectionEvent.call()
        } else {
            sync(service.items)
        }

        service.sessionKillingStateObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }
    }

    private fun sync(state: WalletConnectSessionKillManager.State) {
        when (state) {
            is WalletConnectSessionKillManager.State.Failed -> {
                val errorMessage = if (state.error is UnknownHostException)
                    Translator.getString(R.string.Hud_Text_NoInternet)
                else
                    state.error.message ?: state.error::class.java.simpleName

                killingSessionFailedLiveEvent.postValue(errorMessage)
            }
            WalletConnectSessionKillManager.State.Killed -> {
                killingSessionCompletedLiveEvent.postValue(Unit)
            }
            WalletConnectSessionKillManager.State.NotConnected,
            WalletConnectSessionKillManager.State.Processing -> {
                killingSessionInProcessLiveEvent.postValue(Unit)
            }
        }
    }

    fun onClickDelete(sessionViewItem: WalletConnectViewItem.Session) {
        service.kill(sessionViewItem.session)
    }

    private fun sync(items: List<WalletConnectListService.Item>) {
        val sections = mutableListOf<Section>()
        items.forEach { item ->
            val sessions = item.sessions.map { session ->
                WalletConnectListModule.Session(
                    session = session,
                    title = session.remotePeerMeta.name,
                    subtitle = getSubtitle(item.chain),
                    url = session.remotePeerMeta.url,
                    imageUrl = getSuitableIcon(session.remotePeerMeta.icons),
                )
            }
            sections.add(
                Section(WalletConnectListModule.Version.Version1, sessions)
            )
        }
        sectionsLiveData.postValue(sections)
    }

    private fun getSubtitle(chain: WalletConnectListService.Chain) = when (chain) {
        WalletConnectListService.Chain.Ethereum,
        WalletConnectListService.Chain.Ropsten,
        WalletConnectListService.Chain.Rinkeby,
        WalletConnectListService.Chain.Kovan,
        WalletConnectListService.Chain.Goerli -> "Ethereum"
        WalletConnectListService.Chain.BinanceSmartChain -> "Binance Smart Chain"
    }

    //TODO improve this method
    private fun getSuitableIcon(imageUrls: List<String>): String? {
        return imageUrls.lastOrNull { it.endsWith("png", ignoreCase = true) }
    }

    override fun onCleared() {
        disposables.clear()
    }

    sealed class WalletConnectViewItem {
        class Account(val title: String) : WalletConnectViewItem()

        class Session(
            val session: WalletConnectSession,
            val title: String,
            val url: String,
            val imageUrl: String?,
            val position: ListPosition
        ) : WalletConnectViewItem()
    }

}
