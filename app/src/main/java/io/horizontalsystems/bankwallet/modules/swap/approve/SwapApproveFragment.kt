package io.horizontalsystems.bankwallet.modules.swap.approve

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class SwapApproveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SwapApproveScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val token: Token,
        val requiredAllowance: BigDecimal,
        val spenderAddress: String
    ) : Parcelable
}

@Composable
fun SwapApproveScreen(navController: NavController, input: SwapApproveFragment.Input) {
    val viewModel = viewModel<ApproveViewModel>(
        factory = ApproveViewModel.Factory(
            input.token,
            input.requiredAllowance,
            input.spenderAddress,
        )
    )

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap_Unlock_PageTitle),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = navController::popBackStack
                    )
                )
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        navController.slideFromRightForResult<SwapApproveConfirmationFragment.Result>(
                            R.id.swapApproveConfirmationFragment,
                            SwapApproveConfirmationModule.Input(
                                viewModel.getSendEvmData(),
                                viewModel.blockchainType
                            )
                        ) {
                            navController.setNavigationResultX(it)
                            navController.popBackStack()
                        }
                    },
                )
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
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
            VSpacer(height = 32.dp)
        }
    }
}
