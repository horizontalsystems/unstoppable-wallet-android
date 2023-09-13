package io.horizontalsystems.bankwallet.modules.receivemain

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account

class ReceiveTokenSelectInitViewModel : ViewModel() {
    fun getActiveAccount(): Account? {
        return App.accountManager.activeAccount
    }
}
