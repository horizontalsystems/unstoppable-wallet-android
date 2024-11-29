package io.horizontalsystems.bankwallet.modules.premium

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class PremiumActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            ComposeAppTheme {
                SelectPremiumPlanScreen(
                    navController,
                    onCloseClick = { finish() }
                )
            }
        }
        setStatusBarTransparent()
    }

}