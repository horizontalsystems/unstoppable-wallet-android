package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.RequestType
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Request
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class WC2RequestListViewModel(
    private val service: WC2RequestListService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    val sectionItems = MutableLiveData<List<WC2RequestListModule.SectionViewItem>>(listOf())
    val openRequestLiveEvent = SingleLiveEvent<WC2Request>()
    val errorLiveEvent = SingleLiveEvent<String>()

    init {
        service.itemsObservable
            .subscribeIO { sync(it) }
            .let { disposables.add(it) }

        service.pendingRequestObservable
            .subscribeIO{ wcRequest ->
                openRequestLiveEvent.postValue(wcRequest)
            }.let {
                disposables.add(it)
            }

        service.errorObservable
            .subscribeIO{ error ->
                errorLiveEvent.postValue(error)
            }.let {
                disposables.add(it)
            }

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

    fun onRequestClick(requestViewItem: WC2RequestListModule.RequestViewItem) {
        service.onRequestClick(requestViewItem.requestId)
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
