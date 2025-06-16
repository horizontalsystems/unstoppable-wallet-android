package cash.p.terminal.modules.manageaccount

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.requireInput
import kotlinx.parcelize.Parcelize

class ManageAccountFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = try {
            navController.requireInput<Input>()
        } catch (e: NullPointerException) {
            navController.popBackStack()
            return
        }
        val viewModel =
            viewModel<ManageAccountViewModel>(factory = ManageAccountModule.Factory(input.accountId))
        ManageAccountScreen(
            navController = navController,
            viewState = viewModel.viewState,
            account = viewModel.account,
            onCloseClicked = viewModel::onClose,
            onSaveClicked = viewModel::onSave,
            onNameChanged = viewModel::onChange
        )
    }

    @Parcelize
    data class Input(val accountId: String) : Parcelable
}
