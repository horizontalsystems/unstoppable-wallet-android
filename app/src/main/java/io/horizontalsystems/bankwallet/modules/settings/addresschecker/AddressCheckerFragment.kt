package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class AddressCheckerFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AddressCheckerScreen(
            onCheckAddressClick = {
                navController.navigate(R.id.addressCheckFragment)
            },
            onClose = { navController.popBackStack() }
        )
    }

}

@Composable
fun AddressCheckerScreen(
    onCheckAddressClick: () -> Unit,
    onClose: () -> Unit
) {
    val viewModel = viewModel<AddressCheckerViewModel>(factory = AddressCheckerModule.Factory())
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.SettingsAddressChecker_Title),
        onBack = onClose,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            SectionUniversalLawrence {
                CellUniversal {
                    body_leah(text = stringResource(R.string.SettingsAddressChecker_RecipientCheck))
                    HFillSpacer(minWidth = 8.dp)
                    HsSwitch(
                        checked = uiState.checkEnabled,
                        onCheckedChange = {
                            viewModel.toggleAddressChecking(it)
                        }
                    )
                }
            }
            InfoText(
                text = stringResource(R.string.SettingsAddressChecker_CheckTheRecipientInfo),
            )
            VSpacer(12.dp)
            CellUniversalLawrenceSection(
                listOf({
                    RowUniversal(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onCheckAddressClick
                    ) {
                        body_leah(
                            text = stringResource(R.string.SettingsAddressChecker_CheckAddress),
                            maxLines = 1,
                        )
                        Spacer(Modifier.weight(1f))
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                })
            )
            VSpacer(32.dp)
        }
    }
}


@Preview
@Composable
fun Preview_AddressChecker() {
    ComposeAppTheme {
        AddressCheckerScreen({}, {})
    }
}

class AddressCheckerViewModel(
    private val localStorage: ILocalStorage,
) : ViewModelUiState<AddressCheckerUiState>() {
    private var checkEnabled = localStorage.recipientAddressCheckEnabled

    override fun createState() = AddressCheckerUiState(
        checkEnabled = checkEnabled
    )

    fun toggleAddressChecking(enabled: Boolean) {
        localStorage.recipientAddressCheckEnabled = enabled
        checkEnabled = enabled
        emitState()
    }
}

object AddressCheckerModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressCheckerViewModel(App.localStorage) as T
        }
    }
}

data class AddressCheckerUiState(
    val checkEnabled: Boolean
)