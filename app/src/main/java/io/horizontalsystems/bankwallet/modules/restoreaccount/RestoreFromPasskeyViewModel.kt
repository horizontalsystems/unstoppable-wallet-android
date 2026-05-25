package io.horizontalsystems.bankwallet.modules.restoreaccount

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.normalizeNFKD
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import javax.inject.Inject

@HiltViewModel
class RestoreFromPasskeyViewModel @Inject constructor(
    private val accountFactory: IAccountFactory
) : ViewModel() {

    fun getAccountType(entropy: ByteArray): AccountType {
        val words = Mnemonic().toMnemonic(entropy, Language.English).map { it.normalizeNFKD() }
        return AccountType.Mnemonic(words, "")
    }

    fun getAccountName(accountName: String?): String {
        return accountName ?: accountFactory.getNextAccountName()
    }
}
