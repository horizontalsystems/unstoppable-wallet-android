package io.horizontalsystems.bankwallet.modules.restore.restoreselectpredefinedaccounttype

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreSelectPredefinedAccountTypeViewModel(
        private val service: RestoreSelectPredefinedAccountTypeService,
        private val clearables: List<RestoreSelectPredefinedAccountTypeService>
) : ViewModel() {

    val viewItems: List<ViewItem>
        get() {
            return service.predefinedAccountTypes.map {
                ViewItem(it, it.title, it.coinCodes)
            }
        }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    data class ViewItem(
            val predefinedAccountType: PredefinedAccountType,
            val title: Int,
            val coinCodes: Int
    )
}
