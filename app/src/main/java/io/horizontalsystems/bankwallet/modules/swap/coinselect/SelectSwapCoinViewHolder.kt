package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.databinding.ViewHolderSwapCoinSelectBinding
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem

class SelectSwapCoinViewHolder(
    private val binding: ViewHolderSwapCoinSelectBinding,
    val onClick: (item: CoinBalanceItem) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var coinItem: CoinBalanceItem? = null

    init {
        binding.wrapper.setOnSingleClickListener {
            coinItem?.let {
                onClick(it)
            }
        }
    }

    fun bind(coinItem: CoinBalanceItem, showBottomBorder: Boolean) {
        this.coinItem = coinItem
        binding.bottomShade.isVisible = showBottomBorder

        coinItem.apply {
            binding.coinIcon.setRemoteImage(
                token.coin.iconUrl,
                token.iconPlaceholder
            )
            binding.coinTitle.text = token.coin.name
            binding.coinSubtitle.text = token.coin.code

            binding.coinBalance.text = balance?.let {
                App.numberFormatter.formatCoinFull(it, token.coin.code, 8)
            }

            binding.fiatBalance.text = fiatBalanceValue?.let {
                App.numberFormatter.formatFiatFull(
                    fiatBalanceValue.value,
                    fiatBalanceValue.currency.symbol
                )
            }
        }
    }

}
