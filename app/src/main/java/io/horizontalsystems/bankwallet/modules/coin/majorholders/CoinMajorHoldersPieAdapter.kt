package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_major_holders_pie.*
import kotlin.math.min

class CoinMajorHoldersPieAdapter(private val holders: List<MajorHolderItem.Item>) : RecyclerView.Adapter<ViewHolderItem>() {

    interface Listener {
        fun onItemClick(address: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        return ViewHolderItem(inflate(parent, R.layout.view_holder_coin_major_holders_pie))
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
        holder.bind(holders)
    }
}

class ViewHolderItem(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(holders: List<MajorHolderItem.Item>) {
        val portionMajor = min(holders.sumOf { it.share }.toFloat(), 100f)
        val portionRest = 100 - portionMajor

        majorHolderView.setProportion(portionMajor, portionRest)
        majorHolderPercent.text = App.numberFormatter.format(portionMajor, 0, 2, suffix = "%")
    }
}
