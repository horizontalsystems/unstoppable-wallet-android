package io.horizontalsystems.bankwallet.modules.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.lib.AlertDialogFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.welcome.WelcomeModule

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewModel: LaunchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(LaunchViewModel::class.java)
        viewModel.init()

        viewModel.showNoDeviceLockWarning.observe(this, Observer {
            AlertDialogFragment.newInstance(R.string.Alert_TitleWarning, R.string.Alert_NoDeviceLockDescription, R.string.Alert_Close,
                    object : AlertDialogFragment.Listener {
                        override fun onButtonClick() {
                            finish()
                        }
                    }).show(supportFragmentManager, "no_device_lock_alert")
        })

        viewModel.openWelcomeModule.observe(this, Observer {
            WelcomeModule.startAsNewTask(App.instance)
            finish()
        })

        viewModel.openMainModule.observe(this, Observer {
            MainModule.startAsNewTask(App.instance)
            finish()
        })

        viewModel.openUnlockModule.observe(this, Observer {
            PinModule.startForUnlock(this, REQUEST_CODE_UNLOCK_PIN)
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UNLOCK_PIN) {
            when (resultCode) {
                Activity.RESULT_OK -> viewModel.delegate.didUnlock()
                Activity.RESULT_CANCELED -> viewModel.delegate.didCancelUnlock()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_UNLOCK_PIN = 1
    }
}
