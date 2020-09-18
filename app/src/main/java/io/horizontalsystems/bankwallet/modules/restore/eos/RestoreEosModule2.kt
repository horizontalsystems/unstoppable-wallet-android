package io.horizontalsystems.bankwallet.modules.restore.eos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.AccountType

object RestoreEosModule2 {
    interface  IService{
        @Throws
        fun accountType(account: String, privateKey: String) : AccountType
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreEosService()

            return RestoreEosViewModel2(service, listOf(service)) as T
        }
    }
}
