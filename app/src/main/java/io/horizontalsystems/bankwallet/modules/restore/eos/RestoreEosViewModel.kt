package io.horizontalsystems.bankwallet.modules.restore.eos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.SingleLiveEvent

class RestoreEosViewModel(
        private val service: RestoreEosModule.IService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val accountTypeLiveEvent = SingleLiveEvent<AccountType>()
    val errorLiveData = MutableLiveData<java.lang.Exception>()

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun onProceed(account: String, privateKey: String) {
        try {
            val accountType = service.accountType(account, privateKey)
            accountTypeLiveEvent.postValue(accountType)
        } catch (e: Exception) {
            errorLiveData.postValue(e)
        }
    }
}
