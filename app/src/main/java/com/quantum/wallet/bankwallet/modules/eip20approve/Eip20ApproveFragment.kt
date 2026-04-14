package com.quantum.wallet.bankwallet.modules.eip20approve

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.core.slideFromRightForResult
import com.quantum.wallet.bankwallet.entities.CoinValue
import com.quantum.wallet.bankwallet.modules.evmfee.ButtonsGroupWithShade
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HsCheckbox
import com.quantum.wallet.bankwallet.ui.compose.components.InfoText
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import com.quantum.wallet.bankwallet.ui.compose.components.subhead2_leah
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
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
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.eip20ApproveFragment)
    }


    val viewModel = viewModel<Eip20ApproveViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = Eip20ApproveViewModel.Factory(
            input.token,
            input.requiredAllowance,
            input.spenderAddress,
        )
    )

    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Swap_Approve_PageTitle),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = navController::popBackStack
            )
        ),
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
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
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)

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
                    subhead2_leah(text = stringResource(id = R.string.Swap_Approve_Unlimited))
                }
            }
            InfoText(text = stringResource(R.string.Swap_Approve_Info))
            VSpacer(height = 32.dp)
        }
    }
}
