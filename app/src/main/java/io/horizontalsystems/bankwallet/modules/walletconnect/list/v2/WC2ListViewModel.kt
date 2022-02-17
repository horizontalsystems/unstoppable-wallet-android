package io.horizontalsystems.bankwallet.modules.walletconnect.list.v2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.walletconnect.walletconnectv2.client.WalletConnect
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule.Section
import io.reactivex.disposables.CompositeDisposable

class WC2ListViewModel(
    private val service: WC2ListService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    val sectionsLiveData = MutableLiveData<List<Section>>()

    init {
//        service.itemsObservable
//            .subscribeIO { sync(it) }
//            .let { disposables.add(it) }

        if (service.sessions.isNotEmpty()) {
            sync(service.sessions)
        }
    }

    private fun sync(sessions: List<WalletConnect.Model.SettledSession>) {
        val sections = mutableListOf<Section>()
        sessions.forEach { item ->
            val sessionItems = sessions.map { session ->
                WalletConnectListModule.SessionViewItem(
                    sessionId = session.topic,
                    title = session.peerAppMetaData?.name ?:"",
                    subtitle = item.permissions.blockchain.chains.toString(),
                    url = session.peerAppMetaData?.url ?: "",
                    imageUrl = getSuitableIcon(session.peerAppMetaData?.icons ?: emptyList()),
                )
            }
            sections.add(
                Section(WalletConnectListModule.Version.Version2, sessionItems)
            )
        }
        sectionsLiveData.postValue(sections)
    }

    private fun getSubtitle(chain: WalletConnectListModule.Chain) = when (chain) {
        WalletConnectListModule.Chain.Ethereum,
        WalletConnectListModule.Chain.Ropsten,
        WalletConnectListModule.Chain.Rinkeby,
        WalletConnectListModule.Chain.Kovan,
        WalletConnectListModule.Chain.Goerli -> "Ethereum"
        WalletConnectListModule.Chain.BinanceSmartChain -> "Binance Smart Chain"
    }

    private fun getSuitableIcon(imageUrls: List<String>): String? {
        return imageUrls.lastOrNull { it.endsWith("png", ignoreCase = true) }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun getVersionFromUri(scannedText: String): Int {
        return when {
            scannedText.contains("@1?") -> 1
            scannedText.contains("@2?") -> 2
            else -> 0
        }
    }

}
