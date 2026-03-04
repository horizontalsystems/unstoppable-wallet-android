package io.horizontalsystems.bankwallet.modules.restoreaccount.screens

import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.restoreaccount.RestoreAccountScreen
import kotlinx.serialization.Serializable

@Serializable
abstract class RestoreAccountChildScreen : HSScreen(
    parentScreenClass = RestoreAccountScreen::class
)
