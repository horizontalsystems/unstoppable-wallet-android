package io.horizontalsystems.bankwallet.modules.walletconnect.requestlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object WC2RequestListModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = WC2RequestListService(App.wc2SessionManager, App.accountManager)

            return WC2RequestListViewModel(service) as T
        }
    }

    data class SectionViewItem(
        val accountId: String,
        val walletName: String,
        val active: Boolean,
        val requests: List<RequestViewItem>
    )

    data class RequestViewItem(
        val requestId: Long,
        val title: String,
        val subtitle: String,
    )

    data class RequestItem(
        val id: Long,
        val sessionName: String,
        val method: String?,
        val chainId: String?
    )

    data class Item(
        val accountId: String,
        val accountName: String,
        val active: Boolean,
        val requests: List<RequestItem>
    )

}
