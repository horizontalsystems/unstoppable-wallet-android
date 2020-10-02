package io.horizontalsystems.bankwallet.modules.swap.view

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveFragment
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.modules.swap.confirmation.SwapConfirmationFragment
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.view.item.TradeViewItem
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_swap.*
import java.math.BigDecimal

class SwapFragment : BaseFragment() {

    lateinit var viewModel: SwapViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.setHomeAsUpIndicator(context?.getDrawable(R.drawable.ic_info))
            it.supportActionBar?.title = getString(R.string.Swap)
        }

        val coinSending = arguments?.getParcelable<Coin>("tokenInKey")
        viewModel = ViewModelProvider(requireActivity(), SwapModule.Factory(coinSending!!)).get(SwapViewModel::class.java)

        fromAmount.apply {
            onTokenButtonClick {
                activity?.let {
                    SelectSwapCoinFragment.start(it, SelectType.FromCoin, true, viewModel.coinReceiving.value)
                }
            }

            editText.addTextChangedListener(fromAmountListener)
        }

        toAmount.apply {
            onTokenButtonClick {
                activity?.let {
                    SelectSwapCoinFragment.start(it, SelectType.ToCoin, false, viewModel.coinSending.value)
                }
            }

            editText.addTextChangedListener(toAmountListener)
        }

        proceedButton.setOnSingleClickListener {
            // open confirmation module
            viewModel.onProceedClick()
        }

        setFragmentResultListeners()

        observeViewModel()
    }

    override fun canHandleOnBackPress(): Boolean {
        activity?.supportFragmentManager?.popBackStack()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.swap_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCancel -> {
                activity?.supportFragmentManager?.popBackStack()
                return true
            }
            //todo not working with fragment
            android.R.id.home -> {
                activity?.let { UniswapInfoActivity.start(it) }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setFragmentResultListeners() {
        childFragmentManager.setFragmentResultListener(SwapApproveFragment.requestKey, this) { requestKey, bundle ->
            if (requestKey == SwapApproveFragment.requestKey) {
                val resultOk = bundle.getBoolean(SwapApproveFragment.resultKey)
                if (resultOk) {
                    viewModel.onApproved()
                }
            }
        }

        activity?.supportFragmentManager?.setFragmentResultListener(SelectSwapCoinFragment.requestKey, this) { requestKey, bundle ->
            if (requestKey == SelectSwapCoinFragment.requestKey) {
                val selectType = bundle.getParcelable<SelectType>(SelectSwapCoinFragment.selectTypeResultKey)
                val selectedCoin = bundle.getParcelable<Coin>(SelectSwapCoinFragment.coinResultKey)
                        ?: return@setFragmentResultListener
                when (selectType) {
                    SelectType.FromCoin -> viewModel.setCoinSending(selectedCoin)
                    SelectType.ToCoin -> viewModel.setCoinReceiving(selectedCoin)
                }
            }
        }
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
            approveButton.isVisible = approveData != null
            approveButton.setOnSingleClickListener {
                approveData?.let {
                    SwapApproveFragment
                            .newInstance(it.coin, it.amount, it.spenderAddress)
                            .show(childFragmentManager, "SwapApproveFragment")
                }
            }
        })

        viewModel.openConfirmation.observe(viewLifecycleOwner, Observer { requireConfirmation ->
            if (requireConfirmation) {
                activity?.let {
                    SwapConfirmationFragment.start(it)
                }
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
            HudHelper.showSuccessMessage(requireView(), it, HudHelper.SnackbarDuration.LONG)
            Handler().postDelayed({ activity?.supportFragmentManager?.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE) }, 1200)
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

    companion object {

        const val TAG = "SwapFragment"

        fun start(activity: FragmentActivity?, coin: Coin) {
            activity?.supportFragmentManager?.commit {
                add(R.id.fragmentContainerView, instance(coin))
                addToBackStack(TAG)
            }
        }

        fun instance(coin: Coin): SwapFragment {
            return SwapFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable("tokenInKey", coin)
                }
            }
        }
    }

    @Parcelize
    enum class SelectType: Parcelable{
        FromCoin, ToCoin
    }

}
