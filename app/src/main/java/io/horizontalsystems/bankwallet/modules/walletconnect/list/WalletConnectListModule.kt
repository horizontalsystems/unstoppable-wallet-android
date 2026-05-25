package io.horizontalsystems.bankwallet.modules.walletconnect.list

import io.horizontalsystems.bankwallet.modules.walletconnect.session.WCRequestViewItem

object WalletConnectListModule {

    data class SessionViewItem(
        val sessionTopic: String,
        val title: String,
        val subtitle: String,
        val url: String,
        val imageUrl: String?,
        val pendingRequestsCount: Int = 0,
        val requests: List<WCRequestViewItem>,
    )

    fun getVersionFromUri(scannedText: String): Int {
        return when {
            scannedText.contains("@2") -> 2
            else -> 0
        }
    }

}
