package io.horizontalsystems.bankwallet.modules.restore.restoreselectpredefinedaccounttype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

object RestoreSelectPredefinedAccountTypeModule {
    interface IService{
        val predefinedAccountTypes: List<PredefinedAccountType>
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreSelectPredefinedAccountTypeService(App.predefinedAccountTypeManager)

            return RestoreSelectPredefinedAccountTypeViewModel(service, listOf(service)) as T
        }
    }
}
