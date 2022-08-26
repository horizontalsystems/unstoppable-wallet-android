package io.horizontalsystems.bankwallet.modules.swap.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.swap.SwapBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ApproveStep
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewComposable
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch.OneInchConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapViewModel.ActionState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import java.util.*

private val uuidFrom = UUID.randomUUID().leastSignificantBits
private val uuidTo = UUID.randomUUID().leastSignificantBits

class OneInchFragment : SwapBaseFragment() {

    private val vmFactory by lazy { OneInchModule.Factory(dex) }
    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModelFactory by lazy {
        OneInchModule.AllowanceViewModelFactory(
            oneInchViewModel.service
        )
    }
    private val allowanceViewModel by viewModels<SwapAllowanceViewModel> { allowanceViewModelFactory }
    private val cardsFactory by lazy {
        SwapMainModule.CoinCardViewModelFactory(
            this,
            dex,
            oneInchViewModel.service,
            oneInchViewModel.tradeService
        )
    }

    override fun restoreProviderState(providerState: SwapMainModule.SwapProviderState) {
        oneInchViewModel.restoreProviderState(providerState)
    }

    override fun getProviderState(): SwapMainModule.SwapProviderState {
        return oneInchViewModel.getProviderState()
    }

    private val fromCoinCardViewModel by lazy {
        ViewModelProvider(
            this,
            cardsFactory
        )[SwapMainModule.coinCardTypeFrom, SwapCoinCardViewModel::class.java]
    }

    private val toCoinCardViewModel by lazy {
        ViewModelProvider(
            this,
            cardsFactory
        )[SwapMainModule.coinCardTypeTo, SwapCoinCardViewModel::class.java]
    }

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
                OneInchScreen(
                    viewModel = oneInchViewModel,
                    fromCoinCardViewModel = fromCoinCardViewModel,
                    toCoinCardViewModel = toCoinCardViewModel,
                    allowanceViewModel = allowanceViewModel,
                    navController = findNavController(),
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()

        oneInchViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()

        oneInchViewModel.onStop()
    }

}

private fun getTitle(action: ActionState?): String {
    return when (action) {
        is ActionState.Enabled -> action.title
        is ActionState.Disabled -> action.title
        else -> ""
    }
}

@Composable
private fun OneInchScreen(
    viewModel: OneInchSwapViewModel,
    fromCoinCardViewModel: SwapCoinCardViewModel,
    toCoinCardViewModel: SwapCoinCardViewModel,
    allowanceViewModel: SwapAllowanceViewModel,
    navController: NavController
) {
    val buttons by viewModel.buttonsLiveData().observeAsState()
    val showProgressbar by viewModel.isLoadingLiveData().observeAsState(false)
    val swapError by viewModel.swapErrorLiveData().observeAsState()
    val approveStep by viewModel.approveStepLiveData().observeAsState()

    navController.getNavigationResult(SwapApproveModule.requestKey) {
        if (it.getBoolean(SwapApproveModule.resultKey)) {
            viewModel.didApprove()
        }
    }

    ComposeAppTheme {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {

            Spacer(Modifier.height(12.dp))

            SwapCoinCardViewComposable(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Swap_FromAmountTitle),
                viewModel = fromCoinCardViewModel,
                uuid = uuidFrom,
                amountEnabled = true,
                navController = navController
            )

            SwitchCoinsSection(showProgressbar) { viewModel.onTapSwitch() }

            SwapCoinCardViewComposable(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.Swap_ToAmountTitle),
                viewModel = toCoinCardViewModel,
                uuid = uuidTo,
                amountEnabled = false,
                navController = navController
            )

            SwapAllowance(allowanceViewModel)

            SwapError(swapError)

            ActionButtons(
                buttons = buttons,
                onTapApprove = {
                    viewModel.approveData?.let { data ->
                        navController.slideFromBottom(
                            R.id.swapApproveFragment,
                            SwapApproveModule.prepareParams(data)
                        )
                    }
                },
                onTapProceed = {
                    viewModel.proceedParams?.let { params ->
                        navController.slideFromRight(
                            R.id.oneInchConfirmationFragment,
                            OneInchConfirmationModule.prepareParams(params)
                        )
                    }
                }
            )

            SwapAllowanceSteps(approveStep)
        }
    }
}

@Composable
private fun SwapError(swapError: String?) {
    swapError?.let { error ->
        Spacer(Modifier.height(12.dp))
        AdditionalDataCell2 {
            subhead2_lucian(text = error)
        }
    }
}

@Composable
private fun SwitchCoinsSection(
    showProgressbar: Boolean,
    onSwitchButtonClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        if (showProgressbar) {
            Box(Modifier.padding(top = 8.dp)) {
                HSCircularProgressIndicator()
            }
        }
        HsIconButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = onSwitchButtonClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_switch),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
private fun ActionButtons(
    buttons: OneInchSwapViewModel.Buttons?,
    onTapApprove: () -> Unit,
    onTapProceed: () -> Unit,
) {
    buttons?.let { actionButtons ->
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            if (actionButtons.approve != ActionState.Hidden) {
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = getTitle(actionButtons.approve),
                    onClick = onTapApprove,
                    enabled = actionButtons.approve is ActionState.Enabled
                )
                Spacer(Modifier.width(4.dp))
            }
            ButtonPrimaryYellow(
                modifier = Modifier.weight(1f),
                title = getTitle(actionButtons.proceed),
                onClick = onTapProceed,
                enabled = actionButtons.proceed is ActionState.Enabled
            )
        }
    }
}

@Composable
private fun SwapAllowance(viewModel: SwapAllowanceViewModel) {
    val error by viewModel.isErrorLiveData().observeAsState(false)
    val allowanceAmount by viewModel.allowanceLiveData().observeAsState()
    val visible by viewModel.isVisibleLiveData().observeAsState(false)

    if (visible) {
        Spacer(Modifier.height(12.dp))
        AdditionalDataCell2 {
            subhead2_grey(text = stringResource(R.string.Swap_Allowance))
            Spacer(Modifier.weight(1f))
            allowanceAmount?.let { amount ->
                if (error) {
                    subhead2_lucian(text = amount)
                } else {
                    subhead2_grey(text = amount)
                }
            }
        }
    }
}

@Composable
private fun SwapAllowanceSteps(approveStep: ApproveStep?) {
    val step1Active: Boolean
    val step2Active: Boolean
    when (approveStep) {
        ApproveStep.ApproveRequired, ApproveStep.Approving -> {
            step1Active = true
            step2Active = false
        }
        ApproveStep.Approved -> {
            step1Active = false
            step2Active = true
        }
        ApproveStep.NA, null -> {
            return
        }
    }

    Spacer(Modifier.height(24.dp))
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgeStepCircle(text = "1", active = step1Active)
        Divider(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(ComposeAppTheme.colors.steel20)
                .height(2.dp)
        )
        BadgeStepCircle(text = "2", active = step2Active)
    }
}

@Preview
@Composable
fun Preview_SwapError() {
    ComposeAppTheme {
        SwapError("Swap Error text")
    }
}

@Preview
@Composable
fun Preview_SwitchCoinsSection() {
    ComposeAppTheme {
        SwitchCoinsSection(true, {})
    }
}

@Preview
@Composable
fun Preview_ActionButtons() {
    ComposeAppTheme {
        val buttons = OneInchSwapViewModel.Buttons(
            ActionState.Enabled("Approve"),
            ActionState.Enabled("Proceed")
        )
        ActionButtons(buttons, {}, {})
    }
}

@Preview
@Composable
fun Preview_SwapAllowanceSteps() {
    ComposeAppTheme {
        SwapAllowanceSteps(ApproveStep.ApproveRequired)
    }
}
