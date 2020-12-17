package io.horizontalsystems.bankwallet.modules.market.top

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_sorting.*

class CoinRatesSortingAdapter(
        private val listener: Listener
) : RecyclerView.Adapter<CoinRatesSortingAdapter.SortingViewHolder>() {

    var sortingFieldText = ""
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var sortingPeriodText = ""
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    interface Listener {
        fun onClickSortingField()
        fun onClickSortingPeriod()
    }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortingViewHolder {
        return SortingViewHolder(inflate(parent, R.layout.view_holder_sorting, false), listener)
    }

    override fun onBindViewHolder(holder: SortingViewHolder, position: Int) {
        holder.bind(sortingFieldText, sortingPeriodText)
    }

    class SortingViewHolder(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        init {
            sortingField.setOnSingleClickListener {
                listener.onClickSortingField()
            }
            sortingPeriod.setOnSingleClickListener {
                listener.onClickSortingPeriod()
            }
        }

        fun bind(sortingFieldText: CharSequence, sortingPeriodText: CharSequence) {
            sortingField.text = sortingFieldText
            sortingPeriod.text = sortingPeriodText
        }
    }

}
