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
        fun onClickPeriod()
    }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderHeader {
        return ViewHolderHeader(inflate(parent, R.layout.view_holder_sorting, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderHeader, position: Int) {
        val sortingFieldText = holder.containerView.context.getString(viewModel.sortingField.titleResId)
        val periodText = holder.containerView.context.getString(viewModel.period.titleResId)

        holder.bind(sortingFieldText, periodText)
    }

    class ViewHolderHeader(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        init {
            sortingField.setOnSingleClickListener {
                listener.onClickSortingField()
            }
            period.setOnSingleClickListener {
                listener.onClickPeriod()
            }
        }

        fun bind(sortingFieldText: CharSequence, periodText: CharSequence) {
            if (sortingField.text != sortingFieldText) {
                sortingField.text = sortingFieldText
            }

            if (period.text != periodText) {
                period.text = periodText
            }
        }
    }

}
