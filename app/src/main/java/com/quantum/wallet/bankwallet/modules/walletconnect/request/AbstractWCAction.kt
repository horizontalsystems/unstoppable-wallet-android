package com.quantum.wallet.bankwallet.modules.walletconnect.request

import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.CoroutineScope

abstract class AbstractWCAction : ServiceState<WCActionState>() {
    abstract fun start(coroutineScope: CoroutineScope)
    abstract suspend fun performAction(): String

    abstract fun getTitle(): TranslatableString
    abstract fun getApproveButtonTitle(): TranslatableString
}
