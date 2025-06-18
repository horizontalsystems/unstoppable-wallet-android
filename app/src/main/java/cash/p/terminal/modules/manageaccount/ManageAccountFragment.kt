package cash.p.terminal.modules.manageaccount

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.tangem.ui.accesscoderecovery.AccessCodeRecoveryDialog
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.requireInput
import io.horizontalsystems.core.slideFromRightForResult
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

        LaunchedEffect(Unit) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showAccessCodeRecoveryDialog.collect { card ->
                    navController.slideFromRightForResult<AccessCodeRecoveryDialog.Result>(
                        resId = R.id.accessCodeRecoveryDialog,
                        input = AccessCodeRecoveryDialog.Input(card.userSettings.isUserCodeRecoveryAllowed)
                    ) {

                    }
                }
            }
        }

        ManageAccountScreen(
            navController = navController,
            viewState = viewModel.viewState,
            account = viewModel.account,
            onCloseClicked = viewModel::onClose,
            onSaveClicked = viewModel::onSave,
            onNameChanged = viewModel::onChange,
            onActionClick = viewModel::onActionClick
        )
    }

    @Parcelize
    data class Input(val accountId: String) : Parcelable
}
