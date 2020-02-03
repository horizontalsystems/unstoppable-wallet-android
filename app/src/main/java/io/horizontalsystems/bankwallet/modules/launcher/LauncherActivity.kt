package io.horizontalsystems.bankwallet.modules.launcher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreModule
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenModule
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.welcome.WelcomeModule

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewModel: LaunchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(LaunchViewModel::class.java)
        viewModel.init()

        viewModel.openWelcomeModule.observe(this, Observer {
            WelcomeModule.start(this)
            finish()
        })

        viewModel.openMainModule.observe(this, Observer {
            MainModule.start(this)
            finish()
        })

        viewModel.openUnlockModule.observe(this, Observer {
            LockScreenModule.startForUnlock(this, REQUEST_CODE_UNLOCK_PIN)
        })

        viewModel.openNoSystemLockModule.observe(this, Observer {
            KeyStoreModule.startForNoSystemLock(this)
        })

        viewModel.openKeyInvalidatedModule.observe(this, Observer {
            KeyStoreModule.startForInvalidKey(this)
        })

        viewModel.openUserAuthenticationModule.observe(this, Observer {
            KeyStoreModule.startForUserAuthentication(this)
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UNLOCK_PIN) {
            when (resultCode) {
                PinModule.RESULT_OK -> viewModel.delegate.didUnlock()
                PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlock()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_UNLOCK_PIN = 1
    }
}
