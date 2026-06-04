package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.nav3.Nav3
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.LocalCurrencyManager
import io.horizontalsystems.bankwallet.ui.compose.LocalMarketKit
import io.horizontalsystems.bankwallet.ui.compose.LocalNumberFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject lateinit var numberFormatter: IAppNumberFormatter
    @Inject lateinit var currencyManager: CurrencyManager
    @Inject lateinit var marketKit: MarketKitWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                CompositionLocalProvider(
                    LocalNumberFormatter provides numberFormatter,
                    LocalCurrencyManager provides currencyManager,
                    LocalMarketKit provides marketKit,
                ) {
                    Nav3()
                }
            }
        }
    }
}
