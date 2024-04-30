package io.horizontalsystems.bankwallet.modules.eip20revoke

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.TokenRow
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.BoxBorderedTop
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class Eip20RevokeConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        Eip20RevokeScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val token: Token,
        val spenderAddress: String,
        val allowance: BigDecimal,
    ) : Parcelable

    @Parcelize
    data class Result(val revoked: Boolean) : Parcelable
}

@Composable
fun Eip20RevokeScreen(navController: NavController, input: Eip20RevokeConfirmFragment.Input) {
    val currentBackStackEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.eip20RevokeConfirmFragment)
    }
    val viewModel = viewModel<Eip20RevokeConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry,
        factory = Eip20RevokeConfirmViewModel.Factory(input.token, input.spenderAddress, input.allowance)
    )

    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        onClickBack = navController::popBackStack,
        onClickSettings = {
            navController.slideFromRight(R.id.eip20RevokeTransactionSettingsFragment)
        },
        onClickClose = navController::popBackStack,
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            val view = LocalView.current

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Swap_Revoke),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(
                            view,
                            R.string.Swap_Revoking,
                            SnackbarDuration.INDEFINITE
                        )

                        val result = try {
                            viewModel.revoke()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            Eip20RevokeConfirmFragment.Result(true)
                        } catch (t: Throwable) {
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                            Eip20RevokeConfirmFragment.Result(false)
                        }

                        buttonEnabled = true
                        navController.setNavigationResultX(result)
                        navController.popBackStack()
                    }
                },
                enabled = uiState.revokeEnabled && buttonEnabled
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Cancel),
                onClick = {
                    navController.popBackStack()
                }
            )
        }
    ) {
        SectionUniversalLawrence {
            TokenRow(
                token = uiState.token,
                amount = uiState.allowance,
                fiatAmount = uiState.fiatAmount,
                currency = uiState.currency,
                borderTop = false,
                title = stringResource(R.string.Approve_YouRevoke),
                amountColor = ComposeAppTheme.colors.leah
            )

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
