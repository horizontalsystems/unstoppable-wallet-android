package io.horizontalsystems.bankwallet.modules.swap.coincard

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.setRemoteImage
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinDialogFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_card_swap.view.*
import java.math.BigDecimal
import java.util.*

class SwapCoinCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    CardView(context, attrs, defStyleAttr) {

    private val uuid = UUID.randomUUID().leastSignificantBits

    init {
        radius = LayoutHelper.dpToPx(16f, context)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cardElevation = 0f
        inflate(context, R.layout.view_card_swap, this)
    }

    fun initialize(
        title: String,
        viewModel: SwapCoinCardViewModel,
        fragment: Fragment,
        lifecycleOwner: LifecycleOwner
    ) {
        titleTextView.text = title

        observe(viewModel, lifecycleOwner)

        selectedToken.setOnSingleClickListener {
            val params = SelectSwapCoinDialogFragment.params(uuid, viewModel.dex)
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

        fragment.getNavigationLiveData(SelectSwapCoinDialogFragment.resultBundleKey)
            ?.observe(lifecycleOwner, { bundle ->
                val requestId = bundle.getLong(SelectSwapCoinDialogFragment.requestIdKey)
                val coinBalanceItem =
                    bundle.getParcelable<SwapMainModule.CoinBalanceItem>(SelectSwapCoinDialogFragment.coinBalanceItemResultKey)
                if (requestId == uuid && coinBalanceItem != null) {
                    viewModel.onSelectCoin(coinBalanceItem.platformCoin)
                }
            })
    }

    fun setAmountEnabled(enabled: Boolean) {
        amountInput.setAmountEnabled(enabled)
    }

    private fun observe(viewModel: SwapCoinCardViewModel, lifecycleOwner: LifecycleOwner) {
        viewModel.tokenCodeLiveData().observe(lifecycleOwner, { setCoin(it) })

        viewModel.balanceLiveData().observe(lifecycleOwner, { setBalance(it) })

        viewModel.balanceErrorLiveData().observe(lifecycleOwner, { setBalanceError(it) })

        viewModel.isEstimatedLiveData().observe(lifecycleOwner, { setEstimated(it) })

        viewModel.amountLiveData().observe(lifecycleOwner, { amount ->
            if (!amountsEqual(amount?.toBigDecimalOrNull(), amountInput.getAmount()?.toBigDecimalOrNull())) {
                amountInput.setAmount(amount)
            }
        })

        viewModel.resetAmountLiveEvent().observe(lifecycleOwner) {
            amountInput.setAmount(null, false)
        }

        viewModel.secondaryInfoLiveData().observe(lifecycleOwner, { amountInput.setSecondaryText(it) })
        viewModel.warningInfoLiveData().observe(lifecycleOwner, { amountInput.setWarningText(it) })

        viewModel.inputParamsLiveData().observe(lifecycleOwner, { amountInput.setInputParams(it) })

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

    private fun setCoin(platformCoin: PlatformCoin?) {
        if (platformCoin != null) {
            iconCoin.setRemoteImage(platformCoin.coin.iconUrl, platformCoin.coinType.iconPlaceholder)
            selectedToken.text = platformCoin.code
            selectedToken.setTextColor(context.getColor(R.color.leah))
        } else {
            iconCoin.setImageResource(R.drawable.coin_placeholder)
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

    private fun setEstimated(visible: Boolean) {
        amountInput.setEstimated(visible)
    }

}
