package io.horizontalsystems.bankwallet.modules.swap.uniswap

import android.content.Context
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
import io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap.UniswapConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.getNavigationResult
import kotlinx.android.synthetic.main.fragment_uniswap.*

class UniswapFragment : SwapBaseFragment() {

    private val vmFactory by lazy { UniswapModule.Factory(dex) }
    private val uniswapViewModel by navGraphViewModels<UniswapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModelFactory by lazy {
        UniswapModule.AllowanceViewModelFactory(
            uniswapViewModel.service
        )
    }
    private val allowanceViewModel by viewModels<SwapAllowanceViewModel> { allowanceViewModelFactory }
    private val coinCardViewModelFactory by lazy {
        SwapMainModule.CoinCardViewModelFactory(
            this,
            dex,
            uniswapViewModel.service,
            uniswapViewModel.tradeService
        )
    }

    override fun restoreProviderState(providerState: SwapMainModule.SwapProviderState) {
        uniswapViewModel.restoreProviderState(providerState)
    }

    override fun getProviderState(): SwapMainModule.SwapProviderState {
        return uniswapViewModel.getProviderState()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_uniswap, container, false)
    }

    override fun onStart() {
        super.onStart()

        uniswapViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()

        uniswapViewModel.onStop()
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

        allowanceView.initialize(allowanceViewModel, viewLifecycleOwner)

        observeViewModel()

        getNavigationResult(SwapApproveModule.requestKey)?.let {
            if (it.getBoolean(SwapApproveModule.resultKey)) {
                uniswapViewModel.didApprove()
            }
        }

        switchButton.setOnClickListener {
            uniswapViewModel.onTapSwitch()
        }

        buttonsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun observeViewModel() {
        uniswapViewModel.isLoadingLiveData().observe(viewLifecycleOwner, { isLoading ->
            progressBar.isVisible = isLoading
        })

        uniswapViewModel.swapErrorLiveData().observe(viewLifecycleOwner, { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        uniswapViewModel.tradeViewItemLiveData().observe(viewLifecycleOwner, { tradeViewItem ->
            setTradeViewItem(tradeViewItem)
        })

        uniswapViewModel.buttonsLiveData().observe(viewLifecycleOwner, { buttons ->
            setButtons(buttons)
        })

        uniswapViewModel.openApproveLiveEvent().observe(viewLifecycleOwner, { approveData ->
            SwapApproveModule.start(
                this,
                R.id.swapFragment_to_swapApproveFragment,
                approveData
            )
        })

        uniswapViewModel.openConfirmationLiveEvent().observe(viewLifecycleOwner, { sendEvmData ->
            UniswapConfirmationModule.start(
                requireParentFragment(),
                R.id.swapFragment_to_uniswapConfirmationFragment,
                navOptions(),
                sendEvmData
            )
        })

        uniswapViewModel.approveStepLiveData().observe(viewLifecycleOwner, { approveStep ->
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

    private fun setButtons(buttons: UniswapViewModel.Buttons) {
        val approveButtonVisible = buttons.approve != UniswapViewModel.ActionState.Hidden
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
                                uniswapViewModel.onTapApprove()
                            },
                            enabled = buttons.approve is UniswapViewModel.ActionState.Enabled
                        )
                    }
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .weight(1f)
                            .then(getProceedButtonModifier(approveButtonVisible)),
                        title = getTitle(buttons.proceed),
                        onClick = {
                            uniswapViewModel.onTapProceed()
                        },
                        enabled = buttons.proceed is UniswapViewModel.ActionState.Enabled
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

    private fun getTitle(action: UniswapViewModel.ActionState?): String {
        return when (action) {
            is UniswapViewModel.ActionState.Enabled -> action.title
            is UniswapViewModel.ActionState.Disabled -> action.title
            else -> ""
        }
    }

    private fun setTradeViewItem(tradeViewItem: UniswapViewModel.TradeViewItem?) {
        if (tradeViewItem?.buyPrice != null && tradeViewItem.sellPrice != null) {
            priceViews.isVisible = true
            buyPriceValue.text = tradeViewItem.buyPrice
            sellPriceValue.text = tradeViewItem.sellPrice
        } else {
            priceViews.isVisible = false
        }

        if (tradeViewItem?.priceImpact != null) {
            priceImpactViews.isVisible = true
            priceImpactValue.text = tradeViewItem.priceImpact.value
            priceImpactValue.setTextColor(
                priceImpactColor(
                    requireContext(),
                    tradeViewItem.priceImpact.level
                )
            )
        } else {
            priceImpactViews.isVisible = false
        }

        if (tradeViewItem?.guaranteedAmount != null) {
            guaranteedAmountViews.isVisible = true
            minMaxTitle.text = tradeViewItem.guaranteedAmount.title
            minMaxValue.text = tradeViewItem.guaranteedAmount.value
        } else {
            guaranteedAmountViews.isVisible = false
        }
    }

    private fun priceImpactColor(
        ctx: Context,
        priceImpactLevel: UniswapTradeService.PriceImpactLevel?
    ): Int {
        val color = when (priceImpactLevel) {
            UniswapTradeService.PriceImpactLevel.Normal -> R.color.remus
            UniswapTradeService.PriceImpactLevel.Warning -> R.color.jacob
            UniswapTradeService.PriceImpactLevel.Forbidden -> R.color.lucian
            else -> R.color.grey
        }

        return ctx.getColor(color)
    }

}
