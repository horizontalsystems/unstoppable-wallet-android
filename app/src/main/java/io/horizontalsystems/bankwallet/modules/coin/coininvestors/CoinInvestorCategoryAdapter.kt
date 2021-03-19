package io.horizontalsystems.bankwallet.modules.coin.coininvestors

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.InvestorItem
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_investors_section_header.*
import kotlinx.android.synthetic.main.view_holder_coin_fund_item.*

class CoinInvestorCategoryAdapter(private val items: List<InvestorItem>, private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener{
        fun onItemClick(url: String)
    }

    private val viewTypeHeader = 0
    private val viewTypeFund = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeFund -> ViewHolderCoinInvestorsItem(inflate(parent, R.layout.view_holder_coin_fund_item))
            else -> ViewHolderCoinInvestorsSectionHeader(inflate(parent, R.layout.view_holder_coin_investors_section_header))
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is InvestorItem.Fund -> viewTypeFund
        is InvestorItem.Header -> viewTypeHeader
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when(holder){
            is ViewHolderCoinInvestorsItem -> holder.bind(item as InvestorItem.Fund){
                listener.onItemClick(item.url)
            }
            is ViewHolderCoinInvestorsSectionHeader -> holder.bind(item as InvestorItem.Header)
        }
    }

}


class ViewHolderCoinInvestorsItem(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: InvestorItem.Fund, onClick: () -> Unit) {
        title.text = item.name
        subtitle.text = item.cleanedUrl
        backgroundView.setBackgroundResource(item.position.getBackground())
        containerView.setOnClickListener {
            onClick.invoke()
        }
    }
}

class ViewHolderCoinInvestorsSectionHeader(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: InvestorItem.Header) {
        sectionTitle.text = item.title
    }
}
