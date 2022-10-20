package io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAccountFactory

class RestorePrivateKeyViewModel(
    accountFactory: IAccountFactory,
) : ViewModel() {

    val defaultName = accountFactory.getNextAccountName()

    fun onEnterPrivateKey(scannedText: String) {
        TODO("Not yet implemented")
    }

    fun onProceed() {

    }
}
