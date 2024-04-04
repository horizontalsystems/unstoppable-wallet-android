package io.horizontalsystems.bankwallet.modules.swap.approve

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
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
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

class SwapApproveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val approveData = navController.requireInput<SwapMainModule.ApproveData>()
        SwapApproveScreen(navController, approveData)
    }

}

@Composable
fun SwapApproveScreen(
    navController: NavController,
    approveData: SwapMainModule.ApproveData
) {
    val swapApproveViewModel =
        viewModel<SwapApproveViewModel>(factory = SwapApproveModule.Factory(approveData))

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
                        swapApproveViewModel.getSendEvmData()?.let { sendEvmData ->
                            navController.slideFromRightForResult<SwapApproveConfirmationFragment.Result>(
                                R.id.swapApproveConfirmationFragment,
                                SwapApproveConfirmationModule.Input(
                                    sendEvmData,
                                    swapApproveViewModel.blockchainType
                                )
                            ) {
                                navController.setNavigationResultX(it)
                                navController.popBackStack()
                            }
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
                CellUniversal {
                    HsCheckbox(checked = true) {

                    }
                    HSpacer(width = 16.dp)
                    subhead2_leah(text = swapApproveViewModel.initialAmount)
                }
                CellUniversal(
                    borderTop = true
                ) {
                    HsCheckbox(checked = false) {

                    }
                    HSpacer(width = 16.dp)
                    subhead2_leah(text = stringResource(id = R.string.Swap_Unlock_Unlimited))
                }
            }
            InfoText(text = stringResource(R.string.Swap_Unlock_Info))
            VSpacer(height = 32.dp)
        }
    }
}
