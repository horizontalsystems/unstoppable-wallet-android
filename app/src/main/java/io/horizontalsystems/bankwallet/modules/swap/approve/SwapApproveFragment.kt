package io.horizontalsystems.bankwallet.modules.swap.approve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.dataKey
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class SwapApproveFragment : BaseFragment() {

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
                val approveData = requireArguments().getParcelable<SwapAllowanceService.ApproveData>(dataKey)!!
                SwapApproveScreen(findNavController(), approveData)
            }
        }
    }
}

@Composable
fun SwapApproveScreen(
    navController: NavController,
    approveData: SwapAllowanceService.ApproveData
) {
    val swapApproveViewModel =
        viewModel<SwapApproveViewModel>(factory = SwapApproveModule.Factory(approveData))

    val approveAllowed = swapApproveViewModel.approveAllowed
    val amountError = swapApproveViewModel.amountError

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Approve_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = navController::popBackStack
                    )
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.Approve_Info)
            )

            Spacer(modifier = Modifier.height(12.dp))
            val state = amountError?.let {
                DataState.Error(it)
            }
            var validAmount by rememberSaveable { mutableStateOf(swapApproveViewModel.initialAmount) }
            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = swapApproveViewModel.initialAmount,
                hint = "",
                state = state,
                pasteEnabled = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textPreprocessor = object : TextPreprocessor {
                    override fun process(text: String): String {
                        if (swapApproveViewModel.validateAmount(text)) {
                            validAmount = text
                        } else {
                            // todo: shake animation
                        }
                        return validAmount
                    }
                },
                onValueChange = {
                    swapApproveViewModel.onEnterAmount(it)
                }
            )

            Spacer(modifier = Modifier.weight(1f))
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.Swap_Proceed),
                    onClick = {
                        swapApproveViewModel.getSendEvmData()?.let { sendEvmData ->
                            navController.slideFromRight(
                                R.id.swapApproveConfirmationFragment,
                                SwapApproveConfirmationModule.prepareParams(sendEvmData, swapApproveViewModel.dex.blockchainType)
                            )
                        }
                    },
                    enabled = approveAllowed
                )
            }
        }
    }
}
