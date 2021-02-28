package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable

class WalletConnectListViewModel(
        service: WalletConnectListService,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()
    val viewItemsLiveData = MutableLiveData<List<WalletConnectViewItem>>()

    init {
        service.itemsObservable
                .subscribeIO { sync(it) }
                .let { disposables.add(it) }

        sync(service.items)
    }

    private fun sync(items: List<WalletConnectListService.Item>) {
        val viewItems = mutableListOf<WalletConnectViewItem>()
        items.forEach { item ->
            val accountViewItem = WalletConnectViewItem.Account(
                    title = stringProvider.string(item.predefinedAccountType.title),
                    address = item.address.eip55
            )
            viewItems.add(accountViewItem)

            item.sessions.forEachIndexed { index, session ->
                val sessionViewItem = WalletConnectViewItem.Session(
                        session = session,
                        title = session.remotePeerMeta.name,
                        url = session.remotePeerMeta.url,
                        imageUrl = getSuitableIcon(session.remotePeerMeta.icons),
                        position = ListPosition.getListPosition(item.sessions.size, index)
                )
                viewItems.add(sessionViewItem)
            }
        }
        viewItemsLiveData.postValue(viewItems)
    }

    //TODO improve this method
    private fun getSuitableIcon(imageUrls: List<String>): String? {
        return imageUrls.lastOrNull { it.endsWith("png", ignoreCase = true) }
    }

    sealed class WalletConnectViewItem {
        class Account(
                val title: String,
                val address: String
        ) : WalletConnectViewItem()

        class Session(
                val session: WalletConnectSession,
                val title: String,
                val url: String,
                val imageUrl: String?,
                val position: ListPosition
        ) : WalletConnectViewItem()
    }

}
