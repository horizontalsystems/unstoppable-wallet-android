package io.horizontalsystems.bankwallet.modules.market.top

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_sorting.*
import java.lang.IllegalStateException

class MarketTopHeaderAdapter(
        private val listener: Listener,
        private val viewModel: MarketTopViewModel,
        viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<MarketTopHeaderAdapter.ViewHolderHeader>() {

    init {
        viewModel.marketTopViewItemsLiveData.observe(viewLifecycleOwner, {
            notifyItemChanged(0)
        })
    }

    interface Listener {
        fun onClickSortingField()
        fun onSelectMarketField(marketField: MarketField)
    }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderHeader {
        return ViewHolderHeader(inflate(parent, R.layout.view_holder_sorting, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderHeader, position: Int) {
        val sortingFieldText = holder.containerView.context.getString(viewModel.sortingField.titleResId)

        holder.bind(sortingFieldText)
    }

    class ViewHolderHeader(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        init {
            sortingField.setOnSingleClickListener {
                listener.onClickSortingField()
            }
            marketFieldSelector.setOnCheckedChangeListener { group, checkedId ->
                val marketField = when (checkedId) {
                    R.id.fieldMarketCap -> MarketField.MarketCap
                    R.id.fieldVolume -> MarketField.Volume
                    R.id.fieldPriceDiff -> MarketField.PriceDiff
                    else -> throw IllegalStateException("")
                }

                listener.onSelectMarketField(marketField)
            }
        }

        fun bind(sortingFieldText: CharSequence) {
            if (sortingField.text != sortingFieldText) {
                sortingField.text = sortingFieldText
            }
        }
    }

}
