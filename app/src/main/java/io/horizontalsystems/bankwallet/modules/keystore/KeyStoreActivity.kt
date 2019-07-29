package io.horizontalsystems.bankwallet.modules.keystore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.AlertDialogFragment
import io.horizontalsystems.bankwallet.modules.launcher.LaunchModule

class KeyStoreActivity : AppCompatActivity() {

    private lateinit var viewModel: KeyStoreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getParcelableExtra<KeyStoreModule.ModeType>(KeyStoreModule.MODE)

        viewModel = ViewModelProviders.of(this).get(KeyStoreViewModel::class.java)
        viewModel.init(mode)

        viewModel.showNoSystemLockWarning.observe(this, Observer {
            AlertDialogFragment.newInstance(R.string.Alert_TitleWarning, R.string.Alert_NoDeviceLockDescription, R.string.Alert_Close,
                    object : AlertDialogFragment.Listener {
                        override fun onButtonClick() {
                            viewModel.delegate.onCloseNoSystemLockWarning()
                        }
                    }).show(supportFragmentManager, "no_device_lock_alert")
        })

        viewModel.showInvalidKeyWarning.observe(this, Observer {
            AlertDialogFragment.newInstance(R.string.Alert_KeysInvalidatedTitle, R.string.Alert_KeysInvalidatedDescription, R.string.Alert_Ok,
                    object : AlertDialogFragment.Listener {
                        override fun onButtonClick() {
                            viewModel.delegate.onCloseInvalidKeyWarning()
                        }
                    }).show(supportFragmentManager, "keys_invalidated_alert")
        })

        viewModel.openLaunchModule.observe(this, Observer {
            LaunchModule.start(this)
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }

}
