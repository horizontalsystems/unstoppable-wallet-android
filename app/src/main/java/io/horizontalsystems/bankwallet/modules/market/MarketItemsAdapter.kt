package io.horizontalsystems.bankwallet.modules.market

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setRemoteImage
import io.horizontalsystems.bankwallet.databinding.ViewHolderMarketItemBinding
import java.math.BigDecimal

class MarketItemsAdapter(
    private val listener: ViewHolderMarketItem.Listener,
    itemsLiveData: LiveData<Pair<List<MarketViewItem>, Boolean>>,
    loadingLiveData: LiveData<Boolean>,
    errorLiveData: LiveData<String?>,
    viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketViewItem, ViewHolderMarketItem>(coinRateDiff) {

    init {
        itemsLiveData.observe(viewLifecycleOwner, { (list, scrollToTop) ->
            submitList(list) {
                if (scrollToTop)
                    recyclerView.scrollToPosition(0)
            }
        })
        errorLiveData.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                submitList(listOf())
            }
        })
        loadingLiveData.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                submitList(listOf())
            }
        })
    }

    private lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketItem {
        return ViewHolderMarketItem(
            ViewHolderMarketItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), listener
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolderMarketItem,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(
            getItem(position),
            payloads.firstOrNull { it is MarketViewItem } as? MarketViewItem,
            position == itemCount - 1)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketItem, position: Int) = Unit

    companion object {
        private val coinRateDiff = object : DiffUtil.ItemCallback<MarketViewItem>() {
            override fun areItemsTheSame(
                oldItem: MarketViewItem,
                newItem: MarketViewItem
            ): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(
                oldItem: MarketViewItem,
                newItem: MarketViewItem
            ): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }

            override fun getChangePayload(oldItem: MarketViewItem, newItem: MarketViewItem): Any? {
                return oldItem
            }
        }
    }
}

class ViewHolderMarketItem(
    private val binding: ViewHolderMarketItemBinding,
    private val listener: Listener
) : RecyclerView.ViewHolder(binding.root) {
    private var item: MarketViewItem? = null

    interface Listener {
        fun onItemClick(marketViewItem: MarketViewItem)
    }

    init {
        binding.wrapper.setOnClickListener {
            item?.let {
                listener.onItemClick(it)
            }
        }
    }

    fun bind(item: MarketViewItem, prev: MarketViewItem?, lastItem: Boolean) {
        this.item = item

        binding.bottomBorder.isVisible = lastItem

        if (item.coinUid != prev?.coinUid) {
            binding.icon.setRemoteImage(item.iconUrl, item.iconPlaceHolder)
        }

        if (prev == null || item.rank != prev.rank) {
            if (item.rank == null) {
                binding.rank.isVisible = false
            } else {
                binding.rank.text = item.rank
                binding.rank.isVisible = true
            }
        }

        if (item.coinName != prev?.coinName) {
            binding.title.text = item.coinName
        }

        if (item.coinCode != prev?.coinCode) {
            binding.subtitle.text = item.coinCode
        }

        if (item.coinRate != prev?.coinRate) {
            binding.rate.text = item.coinRate
        }

        if (item.marketDataValue != prev?.marketDataValue) {
            val marketField = item.marketDataValue

            binding.marketFieldCaption.text = when (marketField) {
                is MarketDataValue.MarketCap -> "MCap"
                is MarketDataValue.Volume -> "Vol"
                is MarketDataValue.Diff, is MarketDataValue.DiffNew -> ""
            }

            when (marketField) {
                is MarketDataValue.MarketCap -> {
                    binding.marketFieldValue.text = marketField.value
                    binding.marketFieldValue.setTextColor(
                        binding.wrapper.resources.getColor(
                            R.color.grey,
                            binding.wrapper.context.theme
                        )
                    )
                }
                is MarketDataValue.Volume -> {
                    binding.marketFieldValue.text = marketField.value
                    binding.marketFieldValue.setTextColor(
                        binding.wrapper.resources.getColor(
                            R.color.grey,
                            binding.wrapper.context.theme
                        )
                    )
                }
                is MarketDataValue.Diff -> {
                    val v = marketField.value
                    if (v != null) {
                        val sign = if (v >= BigDecimal.ZERO) "+" else "-"
                        binding.marketFieldValue.text =
                            App.numberFormatter.format(v.abs(), 0, 2, sign, "%")

                        val color = if (v >= BigDecimal.ZERO) R.color.remus else R.color.lucian
                        binding.marketFieldValue.setTextColor(binding.wrapper.context.getColor(color))
                    } else {
                        binding.marketFieldValue.text = "----"
                        binding.marketFieldValue.setTextColor(binding.wrapper.context.getColor(R.color.grey_50))
                    }

                }
            }
        }

    }

}
