package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImportCexAccountViewModel : ViewModel() {
    val cexItems = Cex.all()

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportCexAccountViewModel() as T
        }
    }
}
