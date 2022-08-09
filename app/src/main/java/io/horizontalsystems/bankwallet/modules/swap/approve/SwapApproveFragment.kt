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
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.dataKey
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.requestKey
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.resultKey
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.setNavigationResult

class SwapApproveFragment : BaseFragment() {

    private val vmFactory by lazy {
        val approveData = requireArguments().getParcelable<SwapAllowanceService.ApproveData>(dataKey)!!
        SwapApproveModule.Factory(approveData)
    }
    private val viewModel by navGraphViewModels<SwapApproveViewModel>(R.id.swapApproveFragment) { vmFactory }

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
                SwapApproveScreen(findNavController(), viewModel)
            }
        }
    }
}

@Composable
fun SwapApproveScreen(navController: NavController, swapApproveViewModel: SwapApproveViewModel) {
    val approveAllowed = swapApproveViewModel.approveAllowed
    val amountError = swapApproveViewModel.amountError
    val openConfirmation = swapApproveViewModel.openConfirmation

    openConfirmation?.let { sendEvmData ->
        swapApproveViewModel.openConfirmationProcessed()

        navController.getNavigationResult(requestKey) { result ->
            if (result.getBoolean(resultKey)) {
                navController.setNavigationResult(requestKey, bundleOf(resultKey to true))
                navController.popBackStack(R.id.swapFragment, false)
            }
        }

        navController.slideFromRight(
            R.id.swapApproveConfirmationFragment,
            SwapApproveConfirmationModule.prepareParams(sendEvmData)
        )
    }

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
                    onClick = swapApproveViewModel::onProceed,
                    enabled = approveAllowed
                )
            }
        }
    }
}
