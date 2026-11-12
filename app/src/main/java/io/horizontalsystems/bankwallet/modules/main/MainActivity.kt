package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BaseActivity
import io.horizontalsystems.walletkit.modules.nav3.EntryPage
import io.horizontalsystems.walletkit.modules.nav3.Nav3
import io.horizontalsystems.walletkit.uiv3.exposeTestTags

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        if (!App.localStorage.mainShowedOnce) {
            // The welcome screen opens on the splash screen's own picture and
            // animates from there. The system's exit animation would fade and
            // shift between the two, so the splash is taken away at once.
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                Box(Modifier.exposeTestTags(), propagateMinConstraints = true) {
                    Nav3(EntryPage)
                }
            }
        }
    }
}
