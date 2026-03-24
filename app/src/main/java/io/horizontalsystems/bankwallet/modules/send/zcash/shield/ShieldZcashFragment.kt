package io.horizontalsystems.bankwallet.modules.send.zcash.shield

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.parcelize.Parcelize

class ShieldZcashFragment(val input: Input) : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val viewModel = viewModel<ShieldZcashViewModel>(factory = ShieldZcashModule.Factory(input.wallet))
        ShieldZcashScreen(navController, viewModel, input.entryPointDestId)
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val entryPointDestId: Int
    ) : Parcelable

}
