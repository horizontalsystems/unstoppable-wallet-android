package io.horizontalsystems.bankwallet.modules.backupkey

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.IPinComponent

class BackupKeyService(
        val account: Account,
        private val pinComponent: IPinComponent
) {
    val words: List<String>
    val salt: String
    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            salt = account.type.salt
        } else {
            words = listOf()
            salt = ""
        }
    }

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

}
