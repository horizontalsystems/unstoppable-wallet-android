package io.horizontalsystems.bankwallet.modules.restorelocal

import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
abstract class RestoreLocalChildScreen : HSScreen(
    parentScreenClass = RestoreLocalScreen::class
)