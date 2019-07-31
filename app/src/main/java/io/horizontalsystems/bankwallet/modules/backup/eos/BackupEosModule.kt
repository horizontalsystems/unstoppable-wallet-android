package io.horizontalsystems.bankwallet.modules.backup.eos

import androidx.appcompat.app.AppCompatActivity

object BackupEosModule {
    const val RESULT_SHOW = 1

    interface View

    interface ViewDelegate

    interface Interactor

    interface InteractorDelegate

    interface Router {
        fun close()
    }

    //  helpers

    fun start(context: AppCompatActivity, account: String, activePrivateKey: String) {
        BackupEosActivity.start(context, account, activePrivateKey)
    }
}
