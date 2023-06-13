package cash.p.terminal.modules.importcexaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ImportCexAccountEnterCexDataViewModel(private val cexId: String) : ViewModel() {
    val cex = Cex.getById(cexId)

    class Factory(private val cexId: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportCexAccountEnterCexDataViewModel(cexId) as T
        }
    }

}
