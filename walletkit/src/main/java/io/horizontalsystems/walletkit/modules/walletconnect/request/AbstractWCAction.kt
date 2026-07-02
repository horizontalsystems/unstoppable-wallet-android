package io.horizontalsystems.walletkit.modules.walletconnect.request

import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import kotlinx.coroutines.CoroutineScope

abstract class AbstractWCAction : ServiceState<WCActionState>() {
    abstract fun start(coroutineScope: CoroutineScope)
    abstract suspend fun performAction(): String

    abstract fun getTitle(): TranslatableString
    abstract fun getApproveButtonTitle(): TranslatableString
}
