package io.horizontalsystems.bankwallet.modules.swap_new.coinselect

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem
import io.horizontalsystems.views.inflate

class SelectSwapCoinAdapter(
        private val onClickItem: (CoinBalanceItem) -> Unit
) : RecyclerView.Adapter<SelectSwapCoinViewHolder>() {

    var items = listOf<CoinBalanceItem>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectSwapCoinViewHolder {
        return SelectSwapCoinViewHolder(inflate(parent, R.layout.view_holder_swap_coin_select), onClickItem)
    }

    override fun onBindViewHolder(holder: SelectSwapCoinViewHolder, position: Int) {
        holder.bind(items[position], items.size - 1 == position)
    }

}
