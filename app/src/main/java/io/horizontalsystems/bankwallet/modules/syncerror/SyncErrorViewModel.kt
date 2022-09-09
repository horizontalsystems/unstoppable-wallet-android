package io.horizontalsystems.bankwallet.modules.syncerror

import androidx.lifecycle.ViewModel

class SyncErrorViewModel(
    private val service: SyncErrorService,
) : ViewModel() {

    val sourceChangeable by service::sourceChangeable
    val blockchainWrapper by service::blockchainWrapper
    val coinName by service::coinName
    val reportEmail by service::reportEmail

    fun retry() {
        service.retry()
    }

}
