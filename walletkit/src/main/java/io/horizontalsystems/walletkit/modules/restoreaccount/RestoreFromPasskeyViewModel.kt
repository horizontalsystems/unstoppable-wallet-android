package io.horizontalsystems.walletkit.modules.restoreaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountFactory
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.normalizeNFKD
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic

class RestoreFromPasskeyViewModel(private val accountFactory: IAccountFactory) : ViewModel() {

    fun getAccountType(entropy: ByteArray): AccountType {
        val words = Mnemonic().toMnemonic(entropy, Language.English).map { it.normalizeNFKD() }
        return AccountType.Mnemonic(words, "")
    }

    fun getAccountName(accountName: String?): String {
        return accountName ?: accountFactory.getNextAccountName()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreFromPasskeyViewModel(App.accountFactory) as T
        }
    }
}
