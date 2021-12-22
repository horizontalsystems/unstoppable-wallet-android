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
                platformCoin.coin.iconUrl,
                platformCoin.coinType.iconPlaceholder
            )
            binding.coinTitle.text = platformCoin.name
            binding.coinSubtitle.text = platformCoin.code

            binding.coinBalance.text = balance?.let {
                App.numberFormatter.formatCoin(it, platformCoin.code, 0, 8)
            }

            binding.fiatBalance.text = fiatBalanceValue?.let {
                App.numberFormatter.formatFiat(
                    fiatBalanceValue.value,
                    fiatBalanceValue.currency.symbol,
                    0,
                    2
                )
            }
        }
    }

}
