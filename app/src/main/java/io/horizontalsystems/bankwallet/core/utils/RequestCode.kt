package io.horizontalsystems.bankwallet.core.utils

object ModuleCode {
    const val RESTORE_EOS = 1
    const val RESTORE_WORDS = 2
    const val RESTORE_OPTIONS = 3
    const val BACKUP_WORDS = 4
    const val BACKUP_EOS = 5
    const val UNLOCK_PIN = 6
}

object ModuleField {
    const val WORDS_COUNT = "WORDS_COUNT"
    const val ACCOUNT_TYPE_TITLE = "ACCOUNT_TYPE_TITLE"
    const val SYNCMODE = "SYNCMODE"
    const val ACCOUNT_TYPE = "ACCOUNT_TYPE"
    const val DERIVATION = "DERIVATION"
}
