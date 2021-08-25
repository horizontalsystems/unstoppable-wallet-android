package io.horizontalsystems.bankwallet.modules.transactions

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_filter.*

class TagsAdapter(private val onClick: (Wallet?) -> Unit) :
    ListAdapter<Filter<Wallet>, ViewHolderFilter>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFilter {
        return ViewHolderFilter(inflate(parent, R.layout.view_holder_filter), onClick)
    }

    override fun onBindViewHolder(holder: ViewHolderFilter, position: Int) = Unit

    override fun onBindViewHolder(holder: ViewHolderFilter, position: Int, payloads: MutableList<Any>) {
        val prev = payloads.lastOrNull() as? Filter<Wallet>
        holder.bind(getItem(position), prev)
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Filter<Wallet>>() {
            override fun areItemsTheSame(oldItem: Filter<Wallet>, newItem: Filter<Wallet>) =
                oldItem.item == newItem.item

            override fun areContentsTheSame(oldItem: Filter<Wallet>, newItem: Filter<Wallet>) =
                oldItem.selected == newItem.selected

            override fun getChangePayload(oldItem: Filter<Wallet>, newItem: Filter<Wallet>): Any? {
                return oldItem
            }
        }
    }

}

class ViewHolderFilter(
    override val containerView: View,
    private val onClick: (Wallet?) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var filterWallet: Filter<Wallet>? = null

    init {
        buttonFilter.setOnClickListener {
            filterWallet?.let {
                if (it.selected) {
                    onClick(null)
                } else {
                    onClick(it.item)
                }
            }
        }
    }

    fun bind(current: Filter<Wallet>, prev: Filter<Wallet>?) {
        this.filterWallet = current

        if (current.item.coin.code != prev?.item?.coin?.code) {
            buttonFilter.text = current.item.coin.code
        }

        if (current.selected != prev?.selected) {
            buttonFilter.isActivated = current.selected
        }

    }
}