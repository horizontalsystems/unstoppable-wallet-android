package io.horizontalsystems.bankwallet.core

import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

fun NavBackStack<HSScreen>.addFromBottom(screen: HSScreen) {
    add(screen)
}
