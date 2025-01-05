package cash.p.terminal.modules.eip20approve

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.modules.confirm.ConfirmTransactionScreen
import cash.p.terminal.modules.eip20approve.AllowanceMode.OnlyRequired
import cash.p.terminal.modules.eip20approve.AllowanceMode.Unlimited
import cash.p.terminal.modules.evmfee.Cautions
import cash.p.terminal.modules.multiswap.TokenRow
import cash.p.terminal.modules.multiswap.TokenRowUnlimited
import cash.p.terminal.modules.multiswap.ui.DataFieldFee
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui.compose.components.TransactionInfoContactCell
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.chartview.cell.BoxBorderedTop
import io.horizontalsystems.chartview.cell.SectionUniversalLawrence
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class Eip20ApproveConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        Eip20ApproveConfirmScreen(navController)
    }

    @Parcelize
    data class Result(val approved: Boolean) : Parcelable
}

@Composable
fun Eip20ApproveConfirmScreen(navController: NavController) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.eip20ApproveFragment)
    }
    val viewModel = viewModel<Eip20ApproveViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
    )

    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        onClickBack = navController::popBackStack,
        onClickSettings = {
            navController.slideFromRight(R.id.eip20ApproveTransactionSettingsFragment)
        },
        onClickClose = {
            navController.popBackStack(R.id.eip20ApproveFragment, true)
        },
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            val view = LocalView.current

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Swap_Approve),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(view, R.string.Swap_Approving, SnackbarDuration.INDEFINITE)

                        val result = try {
                            viewModel.approve()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            Eip20ApproveConfirmFragment.Result(true)
                        } catch (t: Throwable) {
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                            Eip20ApproveConfirmFragment.Result(false)
                        }

                        buttonEnabled = true
                        navController.setNavigationResultX(result)
                        navController.popBackStack()
                    }
                },
                enabled = uiState.approveEnabled && buttonEnabled
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Cancel),
                onClick = {
                    navController.popBackStack(R.id.eip20ApproveFragment, true)
                }
            )
        }
    ) {
        SectionUniversalLawrence {
            when (uiState.allowanceMode) {
                OnlyRequired -> {
                    TokenRow(
                        token = uiState.token,
                        amount = uiState.requiredAllowance,
                        fiatAmount = uiState.fiatAmount,
                        currency = uiState.currency,
                        borderTop = false,
                        title = stringResource(R.string.Approve_YouApprove),
                        amountColor = ComposeAppTheme.colors.leah
                    )
                }
                Unlimited -> {
                    TokenRowUnlimited(
                        token = uiState.token,
                        borderTop = false,
                        title = stringResource(R.string.Approve_YouApprove),
                        amountColor = ComposeAppTheme.colors.leah
                    )
                }
            }

            BoxBorderedTop {
                TransactionInfoAddressCell(
                    title = stringResource(R.string.Approve_Spender),
                    value = uiState.spenderAddress,
                    showAdd = uiState.contact == null,
                    blockchainType = uiState.token.blockchainType,
                    navController = navController
                )
            }

            uiState.contact?.let {
                BoxBorderedTop {
                    TransactionInfoContactCell(it.name)
                }
            }
        }

        VSpacer(height = 16.dp)
        SectionUniversalLawrence {
            DataFieldFee(
                navController,
                uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                uiState.networkFee?.secondary?.getFormattedPlain() ?: "---"
            )
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}
