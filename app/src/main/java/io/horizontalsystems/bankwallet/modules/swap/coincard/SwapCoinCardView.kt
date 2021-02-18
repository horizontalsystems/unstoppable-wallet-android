package io.horizontalsystems.bankwallet.modules.swap.coincard

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinDialogFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_card_swap.view.*
import java.math.BigDecimal

class SwapCoinCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr) {

    init {
        radius = LayoutHelper.dpToPx(16f, context)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cardElevation = 0f
        inflate(context, R.layout.view_card_swap, this)
    }

    fun initialize(title: String, viewModel: SwapCoinCardViewModel, fragment: Fragment, lifecycleOwner: LifecycleOwner) {
        titleTextView.text = title

        observe(viewModel, lifecycleOwner)

        selectedToken.setOnSingleClickListener {
            val params = SelectSwapCoinDialogFragment.params(id, ArrayList(viewModel.tokensForSelection))
            fragment.findNavController().navigate(R.id.selectSwapCoinDialog, params)
        }

        amountInput.onTapSecondaryCallback = { viewModel.onSwitch() }

        amountInput.onTextChangeCallback = { old, new ->
            if (viewModel.isValid(new)) {
                viewModel.onChangeAmount(new)
            } else {
                amountInput.revertAmount(old)
            }
        }

        fragment.getNavigationLiveData(SelectSwapCoinDialogFragment.resultBundleKey)?.observe(lifecycleOwner, { bundle ->
            val requestId = bundle.getInt(SelectSwapCoinDialogFragment.requestIdKey)
            val coinBalanceItem = bundle.getParcelable<SwapModule.CoinBalanceItem>(SelectSwapCoinDialogFragment.coinBalanceItemResultKey)
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

        viewModel.amountLiveData().observe(lifecycleOwner, { amount ->
            if (!amountsEqual(amount?.toBigDecimalOrNull(), amountInput.getAmount()?.toBigDecimalOrNull())) {
                amountInput.setAmount(amount)
            }
        })

        viewModel.secondaryInfoLiveData().observe(lifecycleOwner, { amountInput.setSecondaryText(it) })

        viewModel.prefixLiveData().observe(lifecycleOwner, { amountInput.setPrefix(it) })

        viewModel.switchEnabledLiveData().observe(lifecycleOwner, { amountInput.setSecondaryEnabled(it) })

        viewModel.maxEnabledLiveData().observe(lifecycleOwner, { enabled ->
            amountInput.maxButtonVisible = enabled
            if (enabled) {
                amountInput.onTapMaxCallback = { viewModel.onTapMax() }
            }
        })
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
            selectedToken.setTextColor(context.getColor(R.color.leah))
        } else {
            selectedToken.text = context.getString(R.string.Swap_TokenSelectorTitle)
            selectedToken.setTextColor(context.getColor(R.color.jacob))
        }
    }

    private fun setBalance(balance: String?) {
        balanceValue.text = balance
    }

    private fun setBalanceError(show: Boolean) {
        val color = if (show) R.color.lucian else R.color.grey

        balanceTitle.setTextColor(context.getColor(color))
        balanceValue.setTextColor(context.getColor(color))
    }

    private fun setEstimated(show: Boolean) {
        estimatedLabel.isVisible = show
    }

}
