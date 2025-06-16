package cash.p.terminal.tangem.ui

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.tangem.ui.onboarding.OnboardingScreen
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.getInput
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.viewModel

class HardwareWalletOnboardingFragment : BaseComposeFragment() {
    private val viewModel by viewModel<HardwareWalletOnboardingViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        val accountNameInput = navController.getInput<Input>()
        if (accountNameInput == null) {
            navController.popBackStack()
            return
        }

        viewModel.accountName = accountNameInput.name
        OnboardingScreen(
            viewModel = viewModel,
            navController = navController
        )
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable

    @Parcelize
    data class Input(val name: String) : Parcelable
}