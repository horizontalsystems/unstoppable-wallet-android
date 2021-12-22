package io.horizontalsystems.bankwallet.modules.swap.coinselect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.databinding.ViewHolderSwapCoinSelectBinding
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem

class SelectSwapCoinAdapter(
    private val onClickItem: (CoinBalanceItem) -> Unit
) : RecyclerView.Adapter<SelectSwapCoinViewHolder>() {

    var items = listOf<CoinBalanceItem>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectSwapCoinViewHolder {
        return SelectSwapCoinViewHolder(
            ViewHolderSwapCoinSelectBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onClickItem
        )
    }

    override fun onBindViewHolder(holder: SelectSwapCoinViewHolder, position: Int) {
        holder.bind(items[position], items.size - 1 == position)
    }

}
