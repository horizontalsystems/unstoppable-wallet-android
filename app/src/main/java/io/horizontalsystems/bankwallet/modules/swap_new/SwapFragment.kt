package io.horizontalsystems.bankwallet.modules.swap_new

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveFragment
import io.horizontalsystems.bankwallet.modules.swap_new.coincard.SwapCoinCardViewModel
import io.horizontalsystems.bankwallet.modules.swap_new.allowance.SwapAllowanceViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_swap.toolbar
import kotlinx.android.synthetic.main.fragment_swap_new.*

class SwapFragment : BaseFragment() {

    private val vmFactory by lazy { SwapModule.Factory(this, requireArguments().getParcelable(fromCoinKey)!!) }
    private val viewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment) { vmFactory }
    private val allowanceViewModel by navGraphViewModels<SwapAllowanceViewModel>(R.id.swapFragment) { vmFactory }
    private val feeViewModel by navGraphViewModels<EthereumFeeViewModel>(R.id.swapFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        val fromCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeFrom, SwapCoinCardViewModel::class.java)
        val fromCoinCardTitle = getString(R.string.Swap_FromAmountTitle)
        fromCoinCard.initialize(fromCoinCardTitle, fromCoinCardViewModel, this, viewLifecycleOwner)

        val toCoinCardViewModel = ViewModelProvider(this, vmFactory).get(SwapModule.Factory.coinCardTypeTo, SwapCoinCardViewModel::class.java)
        val toCoinCardTile = getString(R.string.Swap_ToAmountTitle)
        toCoinCard.initialize(toCoinCardTile, toCoinCardViewModel, this, viewLifecycleOwner)

        allowanceView.initialize(allowanceViewModel, viewLifecycleOwner)

        approveButton.setOnSingleClickListener {
            viewModel.onTapApprove()
        }

        observeViewModel()

        feeSelectorView.setDurationVisible(false)
        feeSelectorView.setFeeSelectorViewInteractions(feeViewModel, feeViewModel, viewLifecycleOwner, parentFragmentManager)

        getNavigationLiveData(SwapApproveFragment.requestKey)?.observe(viewLifecycleOwner, {
            if (it.getBoolean(SwapApproveFragment.resultKey)) {
                viewModel.didApprove()
            }
        })

        proceedButton.setOnSingleClickListener {
            findNavController().navigate(R.id.swapFragment_to_swapConfirmationFragment, null, navOptions())
        }

//        advancedSettings.setOnSingleClickListener {
//            viewModel.onSettingsClick()
//        }
//
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.swap_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCancel -> {
                findNavController().popBackStack()
                return true
            }
            R.id.menuInfo -> {
                findNavController().navigate(R.id.swapFragment_to_uniswapInfoFragment, null, navOptions())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

        viewModel.proceedAllowedLiveData().observe(viewLifecycleOwner, { proceedAllowed ->
            proceedButton.isEnabled = proceedAllowed
        })

        viewModel.approveActionLiveData().observe(viewLifecycleOwner, { approveActionState ->
            syncApproveButton(approveActionState)
        })

        viewModel.openApproveLiveEvent().observe(viewLifecycleOwner, { approveData ->
            findNavController().navigate(R.id.swapFragment_to_swapApproveFragment, bundleOf(SwapApproveFragment.dataKey to approveData), navOptions())
        })
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

        advancedSettingsViews.isVisible = tradeViewItem != null
    }

    private fun priceImpactColor(ctx: Context, priceImpactLevel: SwapTradeService.PriceImpactLevel?) =
            when (priceImpactLevel) {
                SwapTradeService.PriceImpactLevel.Normal -> LayoutHelper.getAttr(R.attr.ColorRemus, ctx.theme)
                        ?: ctx.getColor(R.color.green_d)
                SwapTradeService.PriceImpactLevel.Warning -> LayoutHelper.getAttr(R.attr.ColorJacob, ctx.theme)
                        ?: ctx.getColor(R.color.yellow_d)
                SwapTradeService.PriceImpactLevel.Forbidden -> LayoutHelper.getAttr(R.attr.ColorLucian, ctx.theme)
                        ?: ctx.getColor(R.color.red_d)
                else -> ctx.getColor(R.color.grey)
            }

    private fun syncApproveButton(approveActionState: SwapViewModel.ApproveActionState) {
        when (approveActionState) {
            SwapViewModel.ApproveActionState.Hidden -> {
                approveButton.isVisible = false
            }
            SwapViewModel.ApproveActionState.Visible -> {
                approveButton.isVisible = true
                approveButton.isEnabled = true
                approveButton.setText(R.string.Swap_Approve)
            }
            SwapViewModel.ApproveActionState.Pending -> {
                approveButton.isVisible = true
                approveButton.isEnabled = false
                approveButton.setText(R.string.Swap_Approving)
            }
        }
    }

    companion object {
        const val fromCoinKey = "fromCoinKey"
    }

}
