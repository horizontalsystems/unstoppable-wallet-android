package io.horizontalsystems.bankwallet.core

import androidx.fragment.app.FragmentManager
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

abstract class BaseComposeFragment(
    screenshotEnabled: Boolean = true
) : HSScreen(screenshotEnabled = screenshotEnabled) {

    val childFragmentManager: FragmentManager
        get() = TODO()

}
