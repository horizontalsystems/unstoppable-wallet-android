package io.horizontalsystems.bankwallet.modules.swap.oneinch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapBaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ApproveStep
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch.OneInchConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapViewModel.ActionState
import io.horizontalsystems.bankwallet.modules.swap.oneinch.OneInchSwapViewModel.Buttons
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.getNavigationResult
import kotlinx.android.synthetic.main.fragment_1inch.*

class OneInchFragment : SwapBaseFragment() {

    private val vmFactory by lazy { OneInchModule.Factory(dex) }
    private val oneInchViewModel by navGraphViewModels<OneInchSwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModelFactory by lazy {
        OneInchModule.AllowanceViewModelFactory(
            oneInchViewModel.service
        )
    }
    private val allowanceViewModel by viewModels<SwapAllowanceViewModel> { allowanceViewModelFactory }
    private val coinCardViewModelFactory by lazy {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_1inch, container, false)
    }

    override fun onStart() {
        super.onStart()

        oneInchViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()

        oneInchViewModel.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fromCoinCardViewModel = ViewModelProvider(this, coinCardViewModelFactory).get(
            SwapMainModule.coinCardTypeFrom,
            SwapCoinCardViewModel::class.java
        )
        fromCoinCard.initialize(
            getString(R.string.Swap_FromAmountTitle),
            fromCoinCardViewModel,
            this,
            viewLifecycleOwner
        )

        val toCoinCardViewModel = ViewModelProvider(this, coinCardViewModelFactory).get(
            SwapMainModule.coinCardTypeTo,
            SwapCoinCardViewModel::class.java
        )
        toCoinCard.initialize(getString(R.string.Swap_ToAmountTitle), toCoinCardViewModel, this, viewLifecycleOwner)
        toCoinCard.setAmountEnabled(false)

        allowanceView.initialize(allowanceViewModel, viewLifecycleOwner)

        observeViewModel()

        getNavigationResult(SwapApproveModule.requestKey)?.let {
            if (it.getBoolean(SwapApproveModule.resultKey)) {
                oneInchViewModel.didApprove()
            }
        }

        switchButton.setOnClickListener {
            oneInchViewModel.onTapSwitch()
        }

        buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun observeViewModel() {
        oneInchViewModel.isLoadingLiveData().observe(viewLifecycleOwner, { isLoading ->
            progressBar.isVisible = isLoading
        })

        oneInchViewModel.swapErrorLiveData().observe(viewLifecycleOwner, { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        oneInchViewModel.buttonsLiveData().observe(viewLifecycleOwner, { buttons ->
            setButtons(buttons)
        })

        oneInchViewModel.openApproveLiveEvent().observe(viewLifecycleOwner, { approveData ->
            SwapApproveModule.start(
                this,
                R.id.swapFragment_to_swapApproveFragment,
                approveData
            )
        })

        oneInchViewModel.openConfirmationLiveEvent()
            .observe(viewLifecycleOwner, { oneInchSwapParameters ->
                OneInchConfirmationModule.start(
                    this,
                    R.id.swapFragment_to_oneInchConfirmationFragment,
                    navOptions(),
                    oneInchSwapParameters
                )
            })

        oneInchViewModel.approveStepLiveData().observe(viewLifecycleOwner, { approveStep ->
            when (approveStep) {
                ApproveStep.ApproveRequired, ApproveStep.Approving -> {
                    approveStepsView.setStepOne()
                }
                ApproveStep.Approved -> {
                    approveStepsView.setStepTwo()
                }
                ApproveStep.NA, null -> {
                    approveStepsView.hide()
                }
            }
        })
    }

    private fun setButtons(buttons: Buttons) {
        val approveButtonVisible = buttons.approve != ActionState.Hidden
        buttonsCompose.setContent {
            ComposeAppTheme {
                Row(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .padding(top = 28.dp, bottom = 24.dp)
                ) {
                    if (approveButtonVisible) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            title = getTitle(buttons.approve),
                            onClick = {
                                oneInchViewModel.onTapApprove()
                            },
                            enabled = buttons.approve is ActionState.Enabled
                        )
                    }
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .weight(1f)
                            .then(getProceedButtonModifier(approveButtonVisible)),
                        title = getTitle(buttons.proceed),
                        onClick = {
                            oneInchViewModel.onTapProceed()
                        },
                        enabled = buttons.proceed is ActionState.Enabled
                    )
                }
            }
        }
    }

    private fun getProceedButtonModifier(approveButtonVisible: Boolean): Modifier {
        return if (approveButtonVisible) {
            Modifier.padding(start = 4.dp)
        } else {
            Modifier.fillMaxWidth()
        }
    }

    private fun getTitle(action: ActionState?): String {
        return when (action) {
            is ActionState.Enabled -> action.title
            is ActionState.Disabled -> action.title
            else -> ""
        }
    }

}
