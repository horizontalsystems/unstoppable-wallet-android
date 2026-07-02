package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.horizontalsystems.walletkit.core.BaseActivity
import io.horizontalsystems.walletkit.modules.nav3.EntryPage
import io.horizontalsystems.walletkit.modules.nav3.Nav3
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                Nav3(EntryPage)
            }
        }
    }
}
