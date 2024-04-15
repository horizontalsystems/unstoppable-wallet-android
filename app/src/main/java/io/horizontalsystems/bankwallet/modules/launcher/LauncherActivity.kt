package io.horizontalsystems.bankwallet.modules.launcher

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule

class LauncherActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        openMain()
    }

    private fun openMain() {
        MainModule.start(this, intent.data)
        intent.data = null
        finish()
    }

}
