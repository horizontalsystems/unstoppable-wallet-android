package io.horizontalsystems.bankwallet.modules.zcashmigration

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Wallet
import kotlinx.parcelize.Parcelize

class ZcashMigrationConfirmFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        input?.let {
            val viewModel = viewModel<ZcashMigrationViewModel>(factory = ZcashMigrationModule.Factory(input.wallet))
            ZcashMigrationConfirmScreen(navController, viewModel)
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet) : Parcelable

}
