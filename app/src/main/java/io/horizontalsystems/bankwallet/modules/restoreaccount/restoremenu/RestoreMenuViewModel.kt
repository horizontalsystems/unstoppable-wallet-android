package io.horizontalsystems.bankwallet.modules.restoreaccount.restoremenu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RestoreMenuViewModel : ViewModel() {

    val restoreOptions = RestoreMenuModule.RestoreOption.values().toList()

    var restoreOption by mutableStateOf(RestoreMenuModule.RestoreOption.RecoveryPhrase)
        private set

    fun onRestoreOptionSelected(option: RestoreMenuModule.RestoreOption) {
        restoreOption = option
    }

}
