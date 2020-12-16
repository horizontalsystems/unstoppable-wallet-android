package io.horizontalsystems.bankwallet.modules.market.top

import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_sorting.*

class CoinRatesSortingAdapter(private val viewModel: MarketTopViewModel) : RecyclerView.Adapter<CoinRatesSortingAdapter.SortingViewHolder>() {

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortingViewHolder {
        return SortingViewHolder(inflate(parent, R.layout.view_holder_sorting, false), viewModel)
    }

    override fun onBindViewHolder(holder: SortingViewHolder, position: Int) = Unit

    class SortingViewHolder(override val containerView: View, private val viewModel: MarketTopViewModel) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        init {
            sortingField.text = viewModel.sortingField
            sortingPeriod.text = viewModel.sortingPeriod

            sortingField.setOnSingleClickListener {
                openFieldSelector()
            }
        }

        private fun openFieldSelector() {
            val items = viewModel.sortingFields.map {
                SelectorItem(it, it == viewModel.sortingField)
            }

            SelectorDialog.newInstance(items, "Sort by") {
                viewModel.sortingField = viewModel.sortingFields[it]
            }
        }
    }

}
