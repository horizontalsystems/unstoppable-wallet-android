package io.horizontalsystems.bankwallet.modules.keystore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.launcher.LaunchModule
import io.horizontalsystems.core.putParcelableExtra
import io.horizontalsystems.keystore.BaseKeyStoreActivity
import io.horizontalsystems.keystore.KeyStoreModule
import io.horizontalsystems.keystore.KeyStoreViewModel
import io.horizontalsystems.keystore.R

class KeyStoreActivity : BaseKeyStoreActivity() {

    override lateinit var viewModel: KeyStoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_keystore)

        val mode = intent.getParcelableExtra<KeyStoreModule.ModeType>(KeyStoreModule.MODE) ?: run {
            finish()
            return
        }

        viewModel = ViewModelProvider(this).get(KeyStoreViewModel::class.java)
        viewModel.init(mode)

        observeEvents()
    }

    override fun openMainModule() {
        LaunchModule.start(this)
    }

    companion object {
        fun startForNoSystemLock(context: Context) {
            start(context, KeyStoreModule.ModeType.NoSystemLock)
        }

        fun startForInvalidKey(context: Context) {
            start(context, KeyStoreModule.ModeType.InvalidKey)
        }

        fun startForDeviceIsRooted(context: Context) {
            start(context, KeyStoreModule.ModeType.DeviceIsRooted)
        }

        fun startForUserAuthentication(context: Context) {
            start(context, KeyStoreModule.ModeType.UserAuthentication)
        }

        private fun start(context: Context, mode: KeyStoreModule.ModeType) {
            val intent = Intent(context, KeyStoreActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

                putParcelableExtra(KeyStoreModule.MODE, mode)
            }

            context.startActivity(intent)
        }
    }
}
