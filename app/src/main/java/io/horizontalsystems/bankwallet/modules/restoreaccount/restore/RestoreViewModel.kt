package io.horizontalsystems.bankwallet.modules.restoreaccount.restore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RestoreViewModel : ViewModel() {

    val restoreOptions = RestoreModule.RestoreOption.values().toList()

    var restoreOption by mutableStateOf(RestoreModule.RestoreOption.RecoveryPhrase)
        private set

    fun onRestoreOptionSelected(option: RestoreModule.RestoreOption) {
        restoreOption = option
    }

}
