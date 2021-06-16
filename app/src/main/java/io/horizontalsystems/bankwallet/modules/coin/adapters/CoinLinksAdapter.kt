package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_link.*

class CoinLinksAdapter(
        linksLiveData: MutableLiveData<List<CoinLink>>,
        viewLifecycleOwner: LifecycleOwner,
        private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        linksLiveData.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()){
                return@observe
            }

            val insertAction = items.isEmpty()
            items = list
            if (insertAction) {
                notifyItemRangeInserted(0, items.size + 1)
            } else {
                notifyItemRangeChanged(0, items.size + 1)
            }
        }
    }

    private var items = listOf<CoinLink>()
    private val viewTypeItem = 0
    private val viewTypeSpacer = 1

    interface Listener {
        fun onClick(coinLink: CoinLink)
    }

    override fun getItemCount(): Int {
        return if (items.isNotEmpty()) items.size + 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> viewTypeSpacer
            else -> viewTypeItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeItem -> ViewHolder(inflate(parent, R.layout.view_holder_coin_link, false), listener)
            viewTypeSpacer -> SpacerViewHolder(inflate(parent, R.layout.view_holder_coin_page_spacer, false))
            else -> throw  IllegalArgumentException("No such viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> holder.bind(items[position-1])
        }
    }

    class ViewHolder(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: CoinLink) {
            linkView.showTitle(item.title)
            linkView.showIcon(ContextCompat.getDrawable(containerView.context, item.icon))
            linkView.setOnClickListener { listener.onClick(item) }
            item.listPosition?.let { linkView.setListPosition(it) }
        }
    }
}
