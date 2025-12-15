package io.horizontalsystems.bankwallet.modules.eip20approve

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.OnlyRequired
import io.horizontalsystems.bankwallet.modules.eip20approve.AllowanceMode.Unlimited
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.TokenRow
import io.horizontalsystems.bankwallet.modules.multiswap.TokenRowUnlimited
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class Eip20ApproveFragment : BaseComposableBottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val navController = findNavController()
                navController.getInput<Input>()?.let { input ->
                    ComposeAppTheme {
                        Eip20ApproveScreen(navController, input)
                    }
                }
            }
        }
    }

    @Parcelize
    data class Input(
        val token: Token,
        val requiredAllowance: BigDecimal,
        val spenderAddress: String
    ) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
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

    BottomSheetContent(
        onDismissRequest = navController::popBackStack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        buttons = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            val view = LocalView.current

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Swap_Approve),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(
                            view,
                            R.string.Swap_Approving,
                            SnackbarDuration.INDEFINITE
                        )

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
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            BottomSheetHeaderV3(
                title = stringResource(R.string.Swap_Approve_PageTitle)
            )

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

                BoxBordered(top = true) {
                    TransactionInfoAddressCell(
                        title = stringResource(R.string.Approve_Spender),
                        value = uiState.spenderAddress,
                        showAdd = uiState.contact == null,
                        blockchainType = uiState.token.blockchainType,
                        navController = navController
                    )
                }

                uiState.contact?.let {
                    BoxBordered(top = true) {
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
}
