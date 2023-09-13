package cash.p.terminal.modules.receivemain

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.App
import cash.p.terminal.entities.Account

class ReceiveTokenSelectInitViewModel : ViewModel() {
    fun getActiveAccount(): Account? {
        return App.accountManager.activeAccount
    }
}
