package cash.p.terminal.modules.resettofactorysettings

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.tangem.ui.HardwareWalletError
import cash.p.terminal.ui.compose.components.HsCheckbox
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryRed
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Account
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResultX
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.viewModel

class ResetToFactorySettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel: ResetToFactorySettingsViewModel by viewModel<ResetToFactorySettingsViewModel>()
        navController.getInput<Input>()?.let {
            viewModel.account = it.account
        }

        val view = LocalView.current
        LaunchedEffect(Unit) {
            viewModel.errorEvents.collect { error ->
                when (error) {
                    HardwareWalletError.UnknownError -> {
                        HudHelper.showErrorMessage(
                            contenView = view,
                            resId = R.string.unknown_error
                        )
                    }

                    HardwareWalletError.AttestationFailed,
                    HardwareWalletError.CardNotActivated,
                    HardwareWalletError.ErrorInBackupCard,
                    is HardwareWalletError.NeedFactoryReset,
                    HardwareWalletError.WalletsNotCreated -> Unit
                }
            }
        }

        LaunchedEffect(viewModel.uiState.value.primaryCardWasReset) {
            if (viewModel.uiState.value.primaryCardWasReset) {
                navController.setNavigationResultX(Result(true))
            }
        }

        LaunchedEffect(viewModel.uiState.value.success) {
            if (viewModel.uiState.value.success) {
                viewModel.deleteAccount()
                navController.popBackStack()
            }
        }

        ResetToFactorySettingsScreen(
            uiState = viewModel.uiState.value,
            navController = navController,
            onResetCardClick = viewModel::resetCards
        )
    }

    @Parcelize
    data class Input(val account: Account) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}

@Composable
private fun ResetToFactorySettingsScreen(
    uiState: ResetToFactorySettingsViewUIState,
    navController: NavController,
    onResetCardClick: () -> Unit
) {
    val confirmations = remember {
        listOf(
            TranslatableString.ResString(R.string.reset_card_to_factory_condition_1),
            TranslatableString.ResString(R.string.reset_card_to_factory_condition_2)
        )
    }
    var checkedItems by remember { mutableStateOf(setOf<Int>()) }
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = stringResource(R.string.reset_to_factory_settings),
            navigationIcon = { HsBackButton(onClick = { navController.popBackStack() }) }
        )
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(confirmations, showFrame = true) { item ->
            val itemId = item.id
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    if (!uiState.primaryCardWasReset) {
                        checkedItems = if (itemId in checkedItems) {
                            checkedItems - itemId
                        } else {
                            checkedItems + itemId
                        }
                    }
                }
            ) {
                HsCheckbox(
                    checked = itemId in checkedItems,
                    enabled = !uiState.primaryCardWasReset,
                    onCheckedChange = { checked ->
                        checkedItems = if (checked) {
                            checkedItems + itemId
                        } else {
                            checkedItems - itemId
                        }
                    },
                )
                Spacer(Modifier.width(16.dp))
                subhead2_leah(
                    text = item.getString()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.reset_card_with_backup_to_factory_message)
        )

        val showFinishButton = uiState.primaryCardWasReset
        Spacer(Modifier.weight(1f))
        ButtonPrimaryRed(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            title = stringResource(
                if (!showFinishButton) {
                    R.string.reset
                } else {
                    R.string.reset_backup_card
                }
            ),
            onClick = onResetCardClick,
            enabled = checkedItems.size == 2
        )
        if (showFinishButton) {
            Spacer(Modifier.height(8.dp))
            ButtonPrimaryDefault(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                title = stringResource(R.string.skip_and_finish),
                onClick = {
                    navController.popBackStack()
                },
                enabled = checkedItems.size == 2
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetToFactorySettingsScreenPreview() {
    ComposeAppTheme {
        ResetToFactorySettingsScreen(
            uiState = ResetToFactorySettingsViewUIState(),
            navController = rememberNavController(),
            onResetCardClick = {}
        )
    }
}