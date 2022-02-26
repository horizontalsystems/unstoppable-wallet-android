package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.RequestType
import io.reactivex.disposables.CompositeDisposable

class WC2RequestListViewModel(
    private val service: WC2RequestListService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    val sectionItems = MutableLiveData<List<WC2RequestListModule.SectionViewItem>>(listOf())

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        sync(service.items)
        service.start()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun onWalletSwitch(accountId: String) {
        service.select(accountId)
    }

    private fun sync(items: List<WC2RequestListModule.Item>) {
        val sections = items.map { item ->
            WC2RequestListModule.SectionViewItem(
                item.accountId,
                item.accountName,
                item.active,
                requests = item.requests.map { request ->
                    WC2RequestListModule.RequestViewItem(
                        request.id,
                        RequestType.fromString(request.method),
                        title(request.method),
                        request.sessionName,
                    )
                }
            )
        }

        sectionItems.postValue(sections)
    }

    private fun title(method: String?): String = when (method) {
        "personal_sign" -> "Personal Sign Request"
        "eth_signTypedData" -> "Typed Sign Request"
        "eth_sendTransaction" -> "Approve Transaction"
        else -> "Unsupported"
    }

}
