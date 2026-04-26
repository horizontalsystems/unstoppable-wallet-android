package cash.p.terminal.modules.settings.advancedsecurity.terms

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.ensurePinSet
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.pin.SetPinFragment
import cash.p.terminal.navigation.slideFromRightForResult
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.HudHelper
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class DeleteContactsTermsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val view = LocalView.current
        val termTitles = stringArrayResource(R.array.delete_all_contacts_terms_checkboxes)
        val viewModel: DeleteContactsTermsViewModel = koinViewModel {
            parametersOf(termTitles)
        }

        DeleteContactsTermsScreen(
            uiState = viewModel.uiState,
            onCheckboxToggle = viewModel::toggleCheckbox,
            onAgreeClick = {
                navController.ensurePinSet(R.string.PinSet_Info) {
                    navController.slideFromRightForResult<SetPinFragment.Result>(
                        R.id.setPinFragment,
                        SetPinFragment.Input(
                            descriptionResId = R.string.pin_set_for_delete_all_contacts,
                            pinType = PinType.DELETE_CONTACTS
                        )
                    ) {
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Created)
                        navController.navigateUp()
                    }
                }
            },
            onNavigateBack = navController::navigateUp
        )
    }
}
