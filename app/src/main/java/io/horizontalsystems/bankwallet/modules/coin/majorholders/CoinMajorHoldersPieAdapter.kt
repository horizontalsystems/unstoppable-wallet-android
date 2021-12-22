package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.databinding.ViewHolderCoinMajorHoldersPieBinding
import io.horizontalsystems.bankwallet.modules.coin.MajorHolderItem
import kotlin.math.min

class CoinMajorHoldersPieAdapter(private val holders: List<MajorHolderItem.Item>) :
    RecyclerView.Adapter<ViewHolderItem>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        return ViewHolderItem(
            ViewHolderCoinMajorHoldersPieBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = 1

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
        holder.bind(holders)
    }
}

class ViewHolderItem(private val binding: ViewHolderCoinMajorHoldersPieBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(holders: List<MajorHolderItem.Item>) {
        val portionMajor = min(holders.sumOf { it.share }.toFloat(), 100f)
        val portionRest = 100 - portionMajor

        binding.majorHolderView.setProportion(portionMajor, portionRest)
        binding.majorHolderPercent.text =
            App.numberFormatter.format(portionMajor, 0, 2, suffix = "%")
    }
}
