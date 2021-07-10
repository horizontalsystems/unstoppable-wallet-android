package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceView
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardView
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.SwapConfirmationModule
import io.horizontalsystems.bankwallet.modules.swap.info.SwapInfoFragment.Companion.dexKey
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.setOnSingleClickListener

class SwapFragment : BaseFragment() {

    private val vmFactory by lazy { SwapModule.Factory(this, requireArguments().getParcelable(fromCoinKey)!!) }
    private val viewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModel by navGraphViewModels<SwapAllowanceViewModel>(R.id.swapFragment) { vmFactory }

    private lateinit var approveButton: Button
    private lateinit var proceedButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var commonError: TextView
    private lateinit var advancedSettingsViews: Group
    private lateinit var poweredBy: TextView
    private lateinit var price: TextView
    private lateinit var priceImpactViews: Group
    private lateinit var priceImpactValue: TextView
    private lateinit var guaranteedAmountViews: Group
    private lateinit var minMaxTitle: TextView
    private lateinit var minMaxValue: TextView
    private lateinit var poweredByLine: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar).setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                R.id.menuInfo -> {
                    findNavController().navigate(R.id.swapFragment_to_swapInfoFragment, bundleOf(dexKey to viewModel.service.dex), navOptions())
                    true
                }
                else -> false
            }
        }

        proceedButton = view.findViewById(R.id.proceedButton)
        progressBar= view.findViewById(R.id.progressBar)
        commonError= view.findViewById(R.id.commonError)
        poweredBy = view.findViewById(R.id.poweredBy)
        price = view.findViewById(R.id.price)
        priceImpactViews = view.findViewById(R.id.priceImpactViews)
        priceImpactValue = view.findViewById(R.id.priceImpactValue)
        guaranteedAmountViews = view.findViewById(R.id.guaranteedAmountViews)
        minMaxTitle = view.findViewById(R.id.minMaxTitle)
        minMaxValue = view.findViewById(R.id.minMaxValue)
        poweredByLine = view.findViewById(R.id.poweredByLine)

        val fromCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeFrom, SwapCoinCardViewModel::class.java)
        val fromCoinCardTitle = getString(R.string.Swap_FromAmountTitle)
        view.findViewById<SwapCoinCardView>(R.id.fromCoinCard).initialize(fromCoinCardTitle, fromCoinCardViewModel, this, viewLifecycleOwner)

        val toCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeTo, SwapCoinCardViewModel::class.java)
        val toCoinCardTile = getString(R.string.Swap_ToAmountTitle)
        view.findViewById<SwapCoinCardView>(R.id.toCoinCard).initialize(toCoinCardTile, toCoinCardViewModel, this, viewLifecycleOwner)

        view.findViewById<SwapAllowanceView>(R.id.allowanceView).initialize(allowanceViewModel, viewLifecycleOwner)

        observeViewModel()

        getNavigationResult(SwapApproveModule.requestKey)?.let {
            if (it.getBoolean(SwapApproveModule.resultKey)) {
                viewModel.didApprove()
            }
        }

        view.findViewById<Button>(R.id.switchButton).setOnClickListener {
            viewModel.onTapSwitch()
        }

        view.findViewById<TextView>(R.id.advancedSettings).setOnSingleClickListener {
            findNavController().navigate(R.id.swapFragment_to_swapTradeOptionsFragment)
        }

        approveButton.setOnSingleClickListener {
            viewModel.onTapApprove()
        }

        proceedButton.setOnSingleClickListener {
            viewModel.onTapProceed()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoadingLiveData().observe(viewLifecycleOwner, { isLoading ->
            progressBar.isVisible = isLoading
        })

        viewModel.swapErrorLiveData().observe(viewLifecycleOwner, { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        viewModel.tradeViewItemLiveData().observe(viewLifecycleOwner, { tradeViewItem ->
            setTradeViewItem(tradeViewItem)
        })

        viewModel.proceedActionLiveData().observe(viewLifecycleOwner, { action ->
            handleButtonAction(proceedButton, action)
        })

        viewModel.approveActionLiveData().observe(viewLifecycleOwner, { approveActionState ->
            handleButtonAction(approveButton, approveActionState)
        })

        viewModel.openApproveLiveEvent().observe(viewLifecycleOwner, { approveData ->
            SwapApproveModule.start(this, R.id.swapFragment_to_swapApproveFragment, navOptions(), approveData)
        })

        viewModel.advancedSettingsVisibleLiveData().observe(viewLifecycleOwner, { visible ->
            advancedSettingsViews.isVisible = visible
        })

        viewModel.openConfirmationLiveEvent().observe(viewLifecycleOwner, { sendEvmData ->
            SwapConfirmationModule.start(this, R.id.swapFragment_to_swapConfirmationFragment, navOptions(), sendEvmData)
        })

        val dexName = when (viewModel.service.dex) {
            SwapModule.Dex.Uniswap -> "Uniswap"
            SwapModule.Dex.PancakeSwap -> "PancakeSwap"
        }
        poweredBy.text = "Powered by $dexName"
    }

    private fun handleButtonAction(button: Button, action: SwapViewModel.ActionState?) {
        when (action) {
            SwapViewModel.ActionState.Hidden -> {
                button.isVisible = false
            }
            is SwapViewModel.ActionState.Enabled -> {
                button.isVisible = true
                button.isEnabled = true
                button.text = action.title
            }
            is SwapViewModel.ActionState.Disabled -> {
                button.isVisible = true
                button.isEnabled = false
                button.text = action.title
            }
        }
    }

    private fun setTradeViewItem(tradeViewItem: SwapViewModel.TradeViewItem?) {
        price.text = tradeViewItem?.price

        if (tradeViewItem?.priceImpact != null) {
            priceImpactViews.isVisible = true
            priceImpactValue.text = tradeViewItem.priceImpact.value
            priceImpactValue.setTextColor(priceImpactColor(requireContext(), tradeViewItem.priceImpact.level))
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
        poweredBy.isVisible = tradeViewItem == null
        poweredByLine.isVisible = tradeViewItem == null
    }

    private fun priceImpactColor(ctx: Context, priceImpactLevel: SwapTradeService.PriceImpactLevel?): Int {
        val color = when (priceImpactLevel) {
            SwapTradeService.PriceImpactLevel.Normal -> R.color.remus
            SwapTradeService.PriceImpactLevel.Warning -> R.color.jacob
            SwapTradeService.PriceImpactLevel.Forbidden -> R.color.lucian
            else -> R.color.grey
        }

        return ctx.getColor(color)
    }

    companion object {
        const val fromCoinKey = "fromCoinKey"
    }

}
