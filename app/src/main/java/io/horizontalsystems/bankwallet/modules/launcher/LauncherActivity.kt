package io.horizontalsystems.bankwallet.modules.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.intro.IntroActivity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.pin.PinModule

class LauncherActivity : AppCompatActivity() {

    private lateinit var viewModel: LaunchViewModel

    private val unlockResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            PinModule.RESULT_OK -> viewModel.delegate.didUnlock()
            PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(LaunchViewModel::class.java)
        viewModel.init()

        viewModel.openWelcomeModule.observe(this, Observer {
            IntroActivity.start(this)
            finish()
        })

        viewModel.openMainModule.observe(this, Observer {
            MainModule.start(this)
            if(App.localStorage.torEnabled) {
                val intent = Intent(this, TorConnectionActivity::class.java)
                startActivity(intent)
            }
            finish()
        })

        viewModel.openUnlockModule.observe(this, Observer {
            val intent = Intent(this, LockScreenActivity::class.java)
            unlockResultLauncher.launch(intent)
        })

        viewModel.openNoSystemLockModule.observe(this, Observer {
            KeyStoreActivity.startForNoSystemLock(this)
        })

        viewModel.openKeyInvalidatedModule.observe(this, Observer {
            KeyStoreActivity.startForInvalidKey(this)
        })

        viewModel.openUserAuthenticationModule.observe(this, Observer {
            KeyStoreActivity.startForUserAuthentication(this)
        })

        viewModel.closeApplication.observe(this, Observer {
            finishAffinity()
        })
    }

}
