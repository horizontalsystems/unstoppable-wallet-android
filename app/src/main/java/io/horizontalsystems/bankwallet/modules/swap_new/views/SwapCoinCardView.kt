package io.horizontalsystems.bankwallet.modules.swap_new.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule
import io.horizontalsystems.bankwallet.modules.swap_new.coinselect.SelectSwapCoinFragment
import io.horizontalsystems.bankwallet.modules.swap_new.viewmodels.SwapCoinCardViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_amount_input.*
import kotlinx.android.synthetic.main.view_card_swap.view.*
import java.math.BigDecimal

class SwapCoinCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private var viewModel: SwapCoinCardViewModel? = null

    init {
        inflate(context, R.layout.view_card_swap, this)
    }

    fun initialize(viewModel: SwapCoinCardViewModel, fragment: Fragment, lifecycleOwner: LifecycleOwner) {
        this.viewModel = viewModel

        title.text = viewModel.title

        viewModel.tokenCodeLiveData().observe(lifecycleOwner, {
            setTokenCode(it)
        })
        viewModel.balanceLiveData().observe(lifecycleOwner, {
            setBalance(it)
        })
        viewModel.isEstimatedLiveData().observe(lifecycleOwner, {
            estimatedLabel.isVisible = it
        })
        viewModel.amountLiveData().observe(lifecycleOwner, {
            updateAmount(it)
        })
        viewModel.revertAmountLiveData().observe(lifecycleOwner, {
            revertAmount(it)
        })

        selectedToken.setOnClickListener {
            val params = SelectSwapCoinFragment.params(id, viewModel.tokensForSelection)
            fragment.findNavController().navigate(R.id.swapFragment_to_selectSwapCoinFragment, params, null)
        }

        fragment.getNavigationLiveData(SelectSwapCoinFragment.resultBundleKey)?.observe(lifecycleOwner, { bundle ->
            val requestId = bundle.getInt(SelectSwapCoinFragment.requestIdKey)
            val coinBalanceItem = bundle.getParcelable<SwapModule.CoinBalanceItem>(SelectSwapCoinFragment.coinBalanceItemResultKey)
            if (requestId == id && coinBalanceItem != null) {
                viewModel.onSelectCoin(coinBalanceItem)
            }
        })

        amount.addTextChangedListener(amountChangeListener)
    }

    private val amountChangeListener = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel?.onChangeAmount(s?.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    private fun updateAmount(amount: String?) {
        this.amount.apply {
//            if (amountsEqual(text?.toString()?.toBigDecimalOrNull(), amount?.toBigDecimalOrNull())) return
            removeTextChangedListener(amountChangeListener)
            setText(amount)
//            amount?.let { setSelection(it.length) }
            addTextChangedListener(amountChangeListener)
        }
    }

//    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
//        return when {
//            amount1 == null && amount2 == null -> true
//            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
//            else -> false
//        }
//    }

    private fun revertAmount(amount: String) {
        this.amount.apply {
            setText(amount)
            setSelection(amount.length)
            val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
            startAnimation(shake)
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

    val amountEditText: EditText
        get() = amount

    fun showEstimated(show: Boolean) {
        estimatedLabel.isVisible = show
    }

    fun showBalanceError(show: Boolean) {
        val color = if (show) {
            LayoutHelper.getAttr(R.attr.ColorLucian, context.theme, context.getColor(R.color.red_d))
        } else {
            context.getColor(R.color.grey)
        }
        balanceTitle.setTextColor(color)
        balanceValue.setTextColor(color)
    }

    fun setSelectedCoin(coin: Coin?) {
        if (coin != null) {
            selectedToken.text = coin.code
            selectedToken.setTextColor(LayoutHelper.getAttr(R.attr.ColorLeah, context.theme, context.getColor(R.color.steel_light)))
        } else {
            selectedToken.text = context.getString(R.string.Swap_TokenSelectorTitle)
            selectedToken.setTextColor(LayoutHelper.getAttr(R.attr.ColorJacob, context.theme, context.getColor(R.color.yellow_d)))
        }
    }

    fun onSelectTokenButtonClick(callback: () -> Unit) {
        selectedToken.setOnClickListener {
            callback()
        }
    }

}
