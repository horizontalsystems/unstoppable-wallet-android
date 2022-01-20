package io.horizontalsystems.bankwallet.modules.swap.coincard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.setRemoteImage
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.databinding.ViewCardSwapBinding
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.coinselect.SelectSwapCoinDialogFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.getNavigationLiveData
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.views.helpers.LayoutHelper
import java.math.BigDecimal
import java.util.*

class SwapCoinCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    CardView(context, attrs, defStyleAttr) {

    private val binding = ViewCardSwapBinding.inflate(LayoutInflater.from(context), this, true)

    private val uuid = UUID.randomUUID().leastSignificantBits

    init {
        radius = LayoutHelper.dpToPx(16f, context)
        layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cardElevation = 0f
    }

    fun initialize(
        title: String,
        viewModel: SwapCoinCardViewModel,
        fragment: Fragment,
        lifecycleOwner: LifecycleOwner
    ) {
        binding.titleTextView.text = title

        observe(viewModel, lifecycleOwner)

        binding.selectedToken.setOnSingleClickListener {
            val params = SelectSwapCoinDialogFragment.prepareParams(uuid, viewModel.dex)
            fragment.findNavController().slideFromBottom(R.id.selectSwapCoinDialog, params)
        }

        binding.amountInput.onTapSecondaryCallback = { viewModel.onSwitch() }

        binding.amountInput.onTextChangeCallback = { old, new ->
            if (viewModel.isValid(new)) {
                viewModel.onChangeAmount(new)
            } else {
                binding.amountInput.revertAmount(old)
            }
        }

        fragment.getNavigationLiveData(SelectSwapCoinDialogFragment.resultBundleKey)
            ?.observe(lifecycleOwner, { bundle ->
                val requestId = bundle.getLong(SelectSwapCoinDialogFragment.requestIdKey)
                val coinBalanceItem =
                    bundle.getParcelable<SwapMainModule.CoinBalanceItem>(
                        SelectSwapCoinDialogFragment.coinBalanceItemResultKey
                    )
                if (requestId == uuid && coinBalanceItem != null) {
                    viewModel.onSelectCoin(coinBalanceItem.platformCoin)
                }
            })
    }

    fun setAmountEnabled(enabled: Boolean) {
        binding.amountInput.setAmountEnabled(enabled)
    }

    private fun observe(viewModel: SwapCoinCardViewModel, lifecycleOwner: LifecycleOwner) {
        viewModel.tokenCodeLiveData().observe(lifecycleOwner, { setCoin(it) })

        viewModel.balanceLiveData().observe(lifecycleOwner, { setBalance(it) })

        viewModel.balanceErrorLiveData().observe(lifecycleOwner, { setBalanceError(it) })

        viewModel.isEstimatedLiveData().observe(lifecycleOwner, { setEstimated(it) })

        viewModel.amountLiveData().observe(lifecycleOwner, { amount ->
            if (!amountsEqual(
                    amount?.toBigDecimalOrNull(),
                    binding.amountInput.getAmount()?.toBigDecimalOrNull()
                )
            ) {
                binding.amountInput.setAmount(amount)
            }
        })

        viewModel.resetAmountLiveEvent().observe(lifecycleOwner) {
            binding.amountInput.setAmount(null, false)
        }

        viewModel.secondaryInfoLiveData()
            .observe(lifecycleOwner, { binding.amountInput.setSecondaryText(it) })
        viewModel.warningInfoLiveData()
            .observe(lifecycleOwner, { binding.amountInput.setWarningText(it) })

        viewModel.inputParamsLiveData()
            .observe(lifecycleOwner, { binding.amountInput.setInputParams(it) })

        viewModel.maxEnabledLiveData().observe(lifecycleOwner, { enabled ->
            binding.amountInput.maxButtonVisible = enabled
            if (enabled) {
                binding.amountInput.onTapMaxCallback = { viewModel.onTapMax() }
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
            binding.iconCoin.setRemoteImage(
                platformCoin.coin.iconUrl,
                platformCoin.coinType.iconPlaceholder
            )
            binding.selectedToken.text = platformCoin.code
            binding.selectedToken.setTextColor(context.getColor(R.color.leah))
        } else {
            binding.iconCoin.setImageResource(R.drawable.coin_placeholder)
            binding.selectedToken.text = context.getString(R.string.Swap_TokenSelectorTitle)
            binding.selectedToken.setTextColor(context.getColor(R.color.jacob))
        }
    }

    private fun setBalance(balance: String?) {
        binding.balanceValue.text = balance
    }

    private fun setBalanceError(show: Boolean) {
        val color = if (show) R.color.lucian else R.color.grey

        binding.balanceTitle.setTextColor(context.getColor(color))
        binding.balanceValue.setTextColor(context.getColor(color))
    }

    private fun setEstimated(visible: Boolean) {
        binding.amountInput.setEstimated(visible)
    }

}
