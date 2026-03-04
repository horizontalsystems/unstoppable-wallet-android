package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreAccountScreen
import kotlinx.serialization.Serializable

@Serializable
abstract class RestoreAccountChildScreen : HSScreen() {
    override fun getParentVMKey(backStack: NavBackStack<HSScreen>): String? {
        return backStack.findLast { it is RestoreAccountScreen }?.toString()
    }
}