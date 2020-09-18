package io.horizontalsystems.bankwallet.modules.restore.eos

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable

class RestoreEosViewModel2(
        private val service: RestoreEosModule2.IService,
        private val clearables: List<Clearable>
) : ViewModel() {

    private var account: String? = null
    private var privateKey: String? = null

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun onEnterAAccount(account: String) {
        this.account = account
    }

    fun onEnterPrivateKey(privateKey: String) {
        this.privateKey = privateKey
    }

    fun onProceed() {
        try {
            val account = account ?: return
            val privateKey = privateKey ?: return
            val accountType = service.accountType(account, privateKey)
            //accountTypeRelay.accept(accountType)
        } catch (e: Exception) {
            //errorRelay.accept(error.convertedError)
        }
    }
}
