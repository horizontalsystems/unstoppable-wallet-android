package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
abstract class ReceiveChooseCoinChildScreen : HSScreen(
    parentScreenClass = ReceiveChooseCoinScreen::class
)