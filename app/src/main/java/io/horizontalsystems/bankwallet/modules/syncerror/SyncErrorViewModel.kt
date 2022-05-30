package io.horizontalsystems.bankwallet.modules.syncerror

import androidx.lifecycle.ViewModel

class SyncErrorViewModel(
    private val service: SyncErrorService,
) : ViewModel() {

    val sourceChangeable by service::sourceChangeable
    val blockchain by service::blockchain
    val coinName by service::coinName
    val reportEmail by service::reportEmail

    fun retry() {
        service.retry()
    }

}
