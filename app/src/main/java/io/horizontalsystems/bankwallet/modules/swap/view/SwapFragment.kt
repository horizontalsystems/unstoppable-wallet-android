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
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule
import io.horizontalsystems.bankwallet.modules.swap.view.item.TradeViewItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_swap.*
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

        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        youPay.apply {
            onSelectTokenButtonClick {
                val params = SelectSwapCoinFragment.params(SelectType.FromCoin, true, viewModel.coinReceiving.value)
                findNavController().navigate(R.id.swapFragment_to_selectSwapCoinFragment, params, navOptions())
            }

            amountEditText.addTextChangedListener(amountSendingListener)
        }

        youGet.apply {
            onSelectTokenButtonClick {
                val params = SelectSwapCoinFragment.params(SelectType.ToCoin, false, viewModel.coinSending.value)
                findNavController().navigate(R.id.swapFragment_to_selectSwapCoinFragment, params, navOptions())
            }

            amountEditText.addTextChangedListener(amountReceivingListener)
        }

        switchButton.setOnClickListener {
            viewModel.onSwitchClick()
        }

        proceedButton.setOnSingleClickListener {
            viewModel.onProceedClick()
        }

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        advancedSettings.setOnSingleClickListener {
            viewModel.onSettingsClick()
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
            R.id.menuInfo -> {
                findNavController().navigate(R.id.swapFragment_to_uniswapInfoFragment, null, navOptions())
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

        getNavigationLiveData(SwapSettingsFragment.requestKey)?.observe(viewLifecycleOwner, { bundle ->
            if (bundle.getBoolean(SwapSettingsFragment.resultKey)) {
                bundle.getParcelable<SwapSettingsModule.SwapSettings>(SwapSettingsFragment.swapSettingsKey)?.let {
                    viewModel.onSwapSettingsUpdated(it)
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.coinSending.observe(viewLifecycleOwner, { coin ->
            youPay.setSelectedCoin(coin)
        })

        viewModel.coinReceiving.observe(viewLifecycleOwner, { coin ->
            youGet.setSelectedCoin(coin)
        })

        viewModel.amountSending.observe(viewLifecycleOwner, { amount ->
            setAmountSendingIfChanged(amount)
        })

        viewModel.amountReceiving.observe(viewLifecycleOwner, { amount ->
            setAmountReceivingIfChanged(amount)
        })

        viewModel.tradeViewItem.observe(viewLifecycleOwner, { tradeViewItem ->
            setTradeViewItem(tradeViewItem)
        })

        viewModel.balanceSending.observe(viewLifecycleOwner, { balance ->
            youPay.setBalance(balance)
        })

        viewModel.balanceReceiving.observe(viewLifecycleOwner, { balance ->
            youGet.setBalance(balance)
        })

        viewModel.allowance.observe(viewLifecycleOwner, { allowance ->
            setAllowance(allowance)
        })

        viewModel.allowanceLoading.observe(viewLifecycleOwner, { isLoading ->
            setAllowanceLoading(isLoading)
        })

        viewModel.insufficientAllowance.observe(viewLifecycleOwner, { error ->
            context?.let {
                val color = if (error)
                    LayoutHelper.getAttr(R.attr.ColorLucian, it.theme) ?: it.getColor(R.color.red_d)
                else
                    it.getColor(R.color.grey)
                allowanceValue.setTextColor(color)
            }
        })

        viewModel.amountSendingError.observe(viewLifecycleOwner, { amountSendingError ->
            youPay.showBalanceError(amountSendingError != null)
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            commonError.text = error
            commonError.isVisible = error != null
        })

        viewModel.amountSendingEstimated.observe(viewLifecycleOwner, { estimated ->
            youPay.showEstimated(estimated)
        })

        viewModel.amountReceivingEstimated.observe(viewLifecycleOwner, { estimated ->
            youGet.showEstimated(estimated)
        })

        viewModel.proceedButtonEnabled.observe(viewLifecycleOwner, { proceedButtonEnabled ->
            proceedButton.isEnabled = proceedButtonEnabled
        })

        viewModel.approveData.observe(viewLifecycleOwner, { approveData ->
            approveButton.isVisible = approveData != null
            approveButton.setOnSingleClickListener {
                approveData?.let {
                    findNavController().navigate(R.id.swapFragment_to_swapApproveFragment, bundleOf(SwapApproveFragment.dataKey to it), navOptions())
                }
            }
        })

        viewModel.openConfirmation.observe(viewLifecycleOwner, { requireConfirmation ->
            if (requireConfirmation) {
                findNavController().navigate(R.id.swapFragment_to_swapConfirmationFragment, null, navOptions())
            }
        })

        viewModel.openSettings.observe(viewLifecycleOwner, { (currentSettings, defaultSettings) ->
            val params = SwapSettingsFragment.params(currentSettings, defaultSettings)
            findNavController().navigate(R.id.swapFragment_to_swapSettingsFragment, params)
        })

        viewModel.loading.observe(viewLifecycleOwner, { isLoading ->
            progressBar.isVisible = isLoading
        })

        viewModel.closeWithSuccess.observe(viewLifecycleOwner, {
            HudHelper.showSuccessMessage(requireView(), it, SnackbarDuration.LONG)
            Handler().postDelayed({
                findNavController().popBackStack(R.id.swapFragment, true)
            }, 1200)
        })
    }

    private fun setAllowance(allowance: String?) {
        allowanceValue.text = allowance
        allowanceGroup.isVisible = allowance != null
    }

    private fun setAllowanceLoading(isLoading: Boolean) {
        if (isLoading) {
            allowanceGroup.isVisible = true
            allowanceValue.text = getString(R.string.Alert_Loading)
            context?.getColor(R.color.grey_50)?.let { allowanceValue.setTextColor(it) }
        }
    }

    private fun setTradeViewItem(tradeViewItem: TradeViewItem?) {
        price.text = tradeViewItem?.price

        priceImpactValue.text = tradeViewItem?.priceImpact
        context?.let {
            priceImpactValue.setTextColor(priceImpactColor(it, tradeViewItem?.priceImpactLevel))
        }

        minMaxTitle.text = tradeViewItem?.minMaxTitle
        minMaxValue.text = tradeViewItem?.minMaxAmount

        tradeDataGroup.isVisible = tradeViewItem != null
    }

    private fun priceImpactColor(ctx: Context, priceImpactLevel: PriceImpact.Level?) =
            when (priceImpactLevel) {
                PriceImpact.Level.Normal -> LayoutHelper.getAttr(R.attr.ColorRemus, ctx.theme)
                        ?: ctx.getColor(R.color.green_d)
                PriceImpact.Level.Warning -> LayoutHelper.getAttr(R.attr.ColorJacob, ctx.theme)
                        ?: ctx.getColor(R.color.yellow_d)
                PriceImpact.Level.Forbidden -> LayoutHelper.getAttr(R.attr.ColorLucian, ctx.theme)
                        ?: ctx.getColor(R.color.red_d)
                else -> ctx.getColor(R.color.grey)
            }

    private val amountSendingListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAmountSending(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private val amountReceivingListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAmountReceiving(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private fun setAmountSendingIfChanged(amount: String?) {
        youPay.amountEditText.apply {
            if (amountsEqual(text?.toString()?.toBigDecimalOrNull(), amount?.toBigDecimalOrNull())) return

            removeTextChangedListener(amountSendingListener)
            setText(amount)
            addTextChangedListener(amountSendingListener)
        }
    }

    private fun setAmountReceivingIfChanged(amount: String?) {
        youGet.amountEditText.apply {
            if (amountsEqual(text?.toString()?.toBigDecimalOrNull(), amount?.toBigDecimalOrNull())) return

            removeTextChangedListener(amountReceivingListener)
            setText(amount)
            addTextChangedListener(amountReceivingListener)
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
