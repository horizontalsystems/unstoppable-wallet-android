package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImportCexAccountEnterCexDataViewModel(cexId: String) : ViewModel() {
    val cex = Cex.getById(cexId)

    class Factory(private val cexId: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportCexAccountEnterCexDataViewModel(cexId) as T
        }
    }

}
