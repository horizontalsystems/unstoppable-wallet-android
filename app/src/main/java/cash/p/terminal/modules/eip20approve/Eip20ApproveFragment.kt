package cash.p.terminal.modules.eip20approve

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.rememberViewModelFromGraph
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.navigation.setNavigationResultX
import cash.p.terminal.navigation.slideFromRightForResult
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversal
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsCheckbox
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.SectionUniversalLawrence
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.headline1_leah
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class Eip20ApproveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            Eip20ApproveScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val token: Token,
        val requiredAllowance: BigDecimal,
        val spenderAddress: String
    ) : Parcelable
}

@Composable
fun Eip20ApproveScreen(navController: NavController, input: Eip20ApproveFragment.Input) {
    val viewModel = rememberViewModelFromGraph<Eip20ApproveViewModel>(
        navController,
        R.id.eip20ApproveFragment,
        Eip20ApproveViewModel.Factory(
            input.token,
            input.requiredAllowance,
            input.spenderAddress,
        )
    ) ?: return

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap_Unlock_PageTitle),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close_24,
                        onClick = navController::popBackStack
                    )
                )
            )
        },
        containerColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxHeight()
        ) {
            VSpacer(height = 12.dp)
            headline1_leah(
                text = stringResource(R.string.Swap_Unlock_Subtitle),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            VSpacer(height = 24.dp)

            SectionUniversalLawrence {
                val setOnlyRequired = { viewModel.setAllowanceMode(AllowanceMode.OnlyRequired) }
                CellUniversal(
                    onClick = setOnlyRequired
                ) {
                    HsCheckbox(
                        checked = uiState.allowanceMode == AllowanceMode.OnlyRequired,
                        onCheckedChange = { setOnlyRequired.invoke() }
                    )
                    HSpacer(width = 16.dp)
                    val coinValue = CoinValue(
                        uiState.token,
                        uiState.requiredAllowance
                    ).getFormattedFull()
                    subhead2_leah(text = coinValue)
                }

                val setUnlimited = { viewModel.setAllowanceMode(AllowanceMode.Unlimited) }
                CellUniversal(
                    borderTop = true,
                    onClick = setUnlimited
                ) {
                    HsCheckbox(
                        checked = uiState.allowanceMode == AllowanceMode.Unlimited,
                        onCheckedChange = { setUnlimited.invoke() }
                    )
                    HSpacer(width = 16.dp)
                    subhead2_leah(text = stringResource(id = R.string.Swap_Unlock_Unlimited))
                }
            }
            InfoText(text = stringResource(R.string.Swap_Unlock_Info))
            Spacer(Modifier.weight(1f))

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.Button_Next),
                onClick = {
                    viewModel.freeze()
                    navController.slideFromRightForResult<Eip20ApproveConfirmFragment.Result>(R.id.eip20ApproveConfirmFragment) {
                        navController.setNavigationResultX(it)
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}
