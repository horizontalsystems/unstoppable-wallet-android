package io.horizontalsystems.walletkit.modules.nav3

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.main.MainActivityViewModel
import io.horizontalsystems.walletkit.modules.main.MainScreenWithRootedDeviceCheck
import io.horizontalsystems.walletkit.modules.transactions.TransactionsModule
import io.horizontalsystems.walletkit.modules.transactions.TransactionsViewModel
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun MainScreen(navigation: HSNavigation, parentScreenContentKey: String) {
    val mainActivityViewModel =
        viewModel<MainActivityViewModel>(viewModelStoreOwner = LocalActivity.current as ComponentActivity)

    val activity = LocalActivity.current

    ChainRegistry.all.forEach { plugin ->
        plugin.MainScreenEffects(navigation)
    }

    MainScreenWithRootedDeviceCheck(
        transactionsViewModel = viewModel<TransactionsViewModel>(factory = TransactionsModule.Factory()),
        navigation = navigation,
        mainActivityViewModel = mainActivityViewModel,
        parentScreenContentKey = parentScreenContentKey
    )
}
