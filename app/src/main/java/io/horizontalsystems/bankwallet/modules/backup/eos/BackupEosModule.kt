package io.horizontalsystems.bankwallet.modules.backup.eos

object BackupEosModule {
    const val requestKey = "key_backup_eos"
    const val requestResult = "key_backup_eos_result"
    const val RESULT_SHOW = 1

    interface View

    interface ViewDelegate

    interface Interactor

    interface InteractorDelegate

    interface Router {
        fun close()
    }
}
