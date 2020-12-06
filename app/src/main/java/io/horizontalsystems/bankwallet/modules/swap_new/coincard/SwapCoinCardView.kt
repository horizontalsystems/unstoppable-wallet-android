package io.horizontalsystems.bankwallet.modules.swap_new.coincard

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule
import io.horizontalsystems.bankwallet.modules.swap_new.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_card_swap.view.*
import java.math.BigDecimal

class SwapCoinCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr) {

    private var viewModel: SwapCoinCardViewModel? = null

    private var onAmountChangeCallback: ((old: String?, new: String?) -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        private var prevValue: String? = null

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onAmountChangeCallback?.invoke(prevValue, s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prevValue = s?.toString()
        }
    }

    init {
        radius = LayoutHelper.dpToPx(16f, context)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cardElevation = 0f
        inflate(context, R.layout.view_card_swap, this)
    }

    fun initialize(title: String, viewModel: SwapCoinCardViewModel, fragment: Fragment, lifecycleOwner: LifecycleOwner) {
        this.viewModel = viewModel

        titleTextView.text = title

        observe(viewModel, lifecycleOwner)

        selectedToken.setOnSingleClickListener {
            val params = SelectSwapCoinFragment.params(id, ArrayList(viewModel.tokensForSelection))
            fragment.findNavController().navigate(R.id.swapFragment_to_selectSwapCoinFragment, params, null)
        }

        amountSwitchButton.setOnSingleClickListener {
            viewModel.onSwitch()
        }

        amount.addTextChangedListener(textWatcher)
        onAmountChangeCallback = { old, new ->
            if (viewModel.isValid(new)) {
                viewModel.onChangeAmount(new)
            } else {
                setAmount(old, true)
            }
        }

        fragment.getNavigationLiveData(SelectSwapCoinFragment.resultBundleKey)?.observe(lifecycleOwner, { bundle ->
            val requestId = bundle.getInt(SelectSwapCoinFragment.requestIdKey)
            val coinBalanceItem = bundle.getParcelable<SwapModule.CoinBalanceItem>(SelectSwapCoinFragment.coinBalanceItemResultKey)
            if (requestId == id && coinBalanceItem != null) {
                viewModel.onSelectCoin(coinBalanceItem.coin)
            }
        })
    }

    private fun observe(viewModel: SwapCoinCardViewModel, lifecycleOwner: LifecycleOwner) {
        viewModel.tokenCodeLiveData().observe(lifecycleOwner, { setTokenCode(it) })

        viewModel.balanceLiveData().observe(lifecycleOwner, { setBalance(it) })

        viewModel.balanceErrorLiveData().observe(lifecycleOwner, { setBalanceError(it) })

        viewModel.isEstimatedLiveData().observe(lifecycleOwner, { setEstimated(it) })

        viewModel.amountLiveData().observe(lifecycleOwner, { setAmount(it) })

        viewModel.switchEnabledLiveData().observe(lifecycleOwner, { amountSwitchButton.isEnabled = it })

        viewModel.secondaryInfoLiveData().observe(lifecycleOwner, { setSecondaryAmountInfo(it) })

        viewModel.prefixLiveData().observe(lifecycleOwner, { setAmountPrefix(it) })
    }

    private fun setAmount(amountText: String?, shakeAnimate: Boolean = false) {
        this.amount.apply {
            if (amountsEqual(text?.toString()?.toBigDecimalOrNull(), amountText?.toBigDecimalOrNull())) return

            removeTextChangedListener(textWatcher)
            setText(amountText)
            addTextChangedListener(textWatcher)

            amountText?.let {
                setSelection(it.length)
            }

            if (shakeAnimate) {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake_edittext))
            }
        }
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

    private fun setTokenCode(code: String?) {
        if (code != null) {
            selectedToken.text = code
            selectedToken.setTextColor(LayoutHelper.getAttr(R.attr.ColorLeah, context.theme, context.getColor(R.color.steel_light)))
        } else {
            selectedToken.text = context.getString(R.string.Swap_TokenSelectorTitle)
            selectedToken.setTextColor(LayoutHelper.getAttr(R.attr.ColorJacob, context.theme, context.getColor(R.color.yellow_d)))
        }
    }

    private fun setBalance(balance: String?) {
        balanceValue.text = balance
    }

    private fun setBalanceError(show: Boolean) {
        val color = if (show) {
            LayoutHelper.getAttr(R.attr.ColorLucian, context.theme, context.getColor(R.color.red_d))
        } else {
            context.getColor(R.color.grey)
        }
        balanceTitle.setTextColor(color)
        balanceValue.setTextColor(color)
    }

    private fun setEstimated(show: Boolean) {
        estimatedLabel.isVisible = show
    }

    private fun setSecondaryAmountInfo(secondaryInfo: SwapCoinCardViewModel.SecondaryInfoViewItem?) {
        secondaryAmount.text = secondaryInfo?.text
        val textColor = if (secondaryInfo?.type == SwapCoinCardViewModel.SecondaryInfoType.Value)
            LayoutHelper.getAttr(R.attr.ColorLeah, context.theme, context.getColor(R.color.steel_light))
        else
            context.getColor(R.color.grey_50)
        secondaryAmount.setTextColor(textColor)
    }

    private fun setAmountPrefix(prefix: String?) {
        amountPrefix.isVisible = prefix != null
        amountPrefix.text = prefix
    }

}
