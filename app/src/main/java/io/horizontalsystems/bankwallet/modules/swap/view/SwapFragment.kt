package io.horizontalsystems.bankwallet.modules.swap.view

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveFragment
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.view.item.TradeViewItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_swap.*
import kotlinx.android.synthetic.main.fragment_swap.toolbar
import java.math.BigDecimal

class SwapFragment : BaseFragment() {

    val viewModel by navGraphViewModels<SwapViewModel>(R.id.swapFragment) {
        SwapModule.Factory(arguments?.getParcelable("tokenInKey")!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        //catch click on top left menu item, Info Icon
        toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.swapFragment_to_uniswapInfoFragment, null, navOptions())
        }

        fromAmount.apply {
            onTokenButtonClick {
                val params = SelectSwapCoinFragment.params(SelectType.FromCoin, true, viewModel.coinReceiving.value)
                findNavController().navigate(R.id.swapFragment_to_selectSwapCoinFragment, params, navOptions())
            }

            editText.addTextChangedListener(fromAmountListener)
        }

        toAmount.apply {
            onTokenButtonClick {
                val params = SelectSwapCoinFragment.params(SelectType.ToCoin, false, viewModel.coinSending.value)
                findNavController().navigate(R.id.swapFragment_to_selectSwapCoinFragment, params, navOptions())
            }

            editText.addTextChangedListener(toAmountListener)
        }

        proceedButton.setOnSingleClickListener {
            // open confirmation module
            viewModel.onProceedClick()
        }

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        setFragmentResultListeners()

        observeViewModel()
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
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setFragmentResultListeners() {
        getNavigationLiveData(SwapApproveFragment.requestKey)?.observe(viewLifecycleOwner, Observer {
            if (it.getBoolean(SwapApproveFragment.resultKey)) {
                viewModel.onApproved()
            }
        })

        getNavigationLiveData(SelectSwapCoinFragment.requestKey)?.observe(viewLifecycleOwner, Observer { bundle ->
            val selectedCoin = bundle.getParcelable<Coin>(SelectSwapCoinFragment.coinResultKey)
            if (selectedCoin != null) {
                when (bundle.getParcelable<SelectType>(SelectSwapCoinFragment.selectTypeResultKey)) {
                    SelectType.FromCoin -> viewModel.setCoinSending(selectedCoin)
                    SelectType.ToCoin -> viewModel.setCoinReceiving(selectedCoin)
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.proceedButtonVisible.observe(viewLifecycleOwner, Observer { proceedButtonVisible ->
            proceedButton.isVisible = proceedButtonVisible
        })

        viewModel.proceedButtonEnabled.observe(viewLifecycleOwner, Observer { proceedButtonEnabled ->
            proceedButton.isEnabled = proceedButtonEnabled
        })

        viewModel.approving.observe(viewLifecycleOwner, Observer { approving ->
            approvingButton.isVisible = approving
            approvingProgressBar.isVisible = approving
        })

        viewModel.approveData.observe(viewLifecycleOwner, Observer { approveData ->
            connectButton.isVisible = approveData != null
            connectButton.setOnSingleClickListener {
                approveData?.let {
                    findNavController().navigate(R.id.swapFragment_to_swapApproveFragment, bundleOf(SwapApproveFragment.dataKey to it), navOptions())
                }
            }
        })

        viewModel.openConfirmation.observe(viewLifecycleOwner, Observer { requireConfirmation ->
            if (requireConfirmation) {
                findNavController().navigate(R.id.swapFragment_to_swapConfirmationFragment, null, navOptions())
            }
        })

        viewModel.coinSending.observe(viewLifecycleOwner, Observer { coin ->
            fromAmount.setSelectedCoin(coin?.code)
        })

        viewModel.coinReceiving.observe(viewLifecycleOwner, Observer { coin ->
            toAmount.setSelectedCoin(coin?.code)
        })

        viewModel.amountSending.observe(viewLifecycleOwner, Observer { amount ->
            setAmountSendingIfChanged(amount)
        })

        viewModel.amountReceiving.observe(viewLifecycleOwner, Observer { amount ->
            setAmountReceivingIfChanged(amount)
        })

        viewModel.balance.observe(viewLifecycleOwner, Observer { balance ->
            availableBalanceValue.text = balance
        })

        viewModel.amountSendingError.observe(viewLifecycleOwner, Observer { amountSendingError ->
            fromAmount.setError(amountSendingError)
        })

        viewModel.amountSendingLabelVisible.observe(viewLifecycleOwner, Observer { isVisible ->
            fromAmountLabel.isVisible = isVisible
        })

        viewModel.amountReceivingLabelVisible.observe(viewLifecycleOwner, Observer { isVisible ->
            toAmountLabel.isVisible = isVisible
        })

        viewModel.tradeViewItem.observe(viewLifecycleOwner, Observer { tradeViewItem ->
            setTradeViewItem(tradeViewItem)
        })

        viewModel.tradeViewItemLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            tradeViewItemProgressBar.isVisible = isLoading
        })

        viewModel.feeLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            feeProgressBar.isVisible = isLoading
        })

        viewModel.allowance.observe(viewLifecycleOwner, Observer { allowance ->
            setAllowance(allowance)
        })

        viewModel.allowanceLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            setAllowanceLoading(isLoading)
        })

        viewModel.insufficientAllowance.observe(viewLifecycleOwner, Observer { error ->
            context?.let {
                val color = if (error)
                    LayoutHelper.getAttr(R.attr.ColorLucian, it.theme) ?: it.getColor(R.color.red_d)
                else
                    it.getColor(R.color.grey)

                allowanceValue.setTextColor(color)
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        viewModel.closeWithSuccess.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(requireView(), it, SnackbarDuration.LONG)
            Handler().postDelayed({
                findNavController().popBackStack()
            }, 1200)
        })
    }

    private fun setAllowance(allowance: String?) {
        allowanceValue.text = allowance
        val isVisible = allowance != null
        allowanceTitle.isVisible = isVisible
        allowanceValue.isVisible = isVisible
    }

    private fun setAllowanceLoading(isLoading: Boolean) {
        if (isLoading) {
            allowanceTitle.isVisible = true
            allowanceValue.isVisible = true
            allowanceValue.text = getString(R.string.Alert_Loading)
            context?.getColor(R.color.grey_50)?.let { allowanceValue.setTextColor(it) }
        }
    }

    private fun setTradeViewItem(tradeViewItem: TradeViewItem?) {
        priceValue.text = tradeViewItem?.price

        priceImpactValue.text = tradeViewItem?.priceImpact
        context?.let {
            priceImpactValue.setTextColor(priceImpactColor(it, tradeViewItem?.priceImpactLevel))
        }

        minMaxTitle.text = tradeViewItem?.minMaxTitle
        minMaxValue.text = tradeViewItem?.minMaxAmount

        setTradeViewItemVisibility(visible = tradeViewItem != null)
    }

    private fun priceImpactColor(ctx: Context, priceImpactLevel: PriceImpact.Level?): Int {
        return when (priceImpactLevel) {
            PriceImpact.Level.Normal -> LayoutHelper.getAttr(R.attr.ColorRemus, ctx.theme)
                    ?: ctx.getColor(R.color.green_d)
            PriceImpact.Level.Warning -> LayoutHelper.getAttr(R.attr.ColorJacob, ctx.theme)
                    ?: ctx.getColor(R.color.yellow_d)
            PriceImpact.Level.Forbidden -> LayoutHelper.getAttr(R.attr.ColorLucian, ctx.theme)
                    ?: ctx.getColor(R.color.red_d)
            else -> ctx.getColor(R.color.grey)
        }
    }

    private fun setTradeViewItemVisibility(visible: Boolean) {
        priceTitle.isVisible = visible
        priceValue.isVisible = visible
        priceImpactTitle.isVisible = visible
        priceImpactValue.isVisible = visible
        minMaxTitle.isVisible = visible
        minMaxValue.isVisible = visible
    }

    private val fromAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAmountSending(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val toAmountListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAmountReceiving(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private fun setAmountSendingIfChanged(amount: String?) {
        fromAmount.editText.apply {
            if (amountsEqual(text?.toString()?.toBigDecimalOrNull(), amount?.toBigDecimalOrNull())) return

            removeTextChangedListener(fromAmountListener)
            setText(amount)
            addTextChangedListener(fromAmountListener)
        }
    }

    private fun setAmountReceivingIfChanged(amount: String?) {
        toAmount.editText.apply {
            if (amountsEqual(text?.toString()?.toBigDecimalOrNull(), amount?.toBigDecimalOrNull())) return

            removeTextChangedListener(toAmountListener)
            setText(amount)
            addTextChangedListener(toAmountListener)
        }
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    @Parcelize
    enum class SelectType : Parcelable {
        FromCoin, ToCoin
    }
}
