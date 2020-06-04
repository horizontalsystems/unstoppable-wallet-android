package io.horizontalsystems.bankwallet.modules.ratelist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.setCoinImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_rate.*

class CoinRatesAdapter(private val listener: Listener) : ListAdapter<ViewItem, RecyclerView.ViewHolder>(coinRateDiff) {

    interface Listener {
        fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem)
    }

    private val coinViewItem = 1
    private val sourceView = 4

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ViewItem.CoinViewItem -> coinViewItem
            is ViewItem.SourceText -> sourceView
            else -> throw UnsupportedOperationException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            coinViewItem -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
            sourceView -> ViewHolderSource(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_list_source, parent, false))
            else -> throw Exception("No such view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item is ViewItem.CoinViewItem) {
            (holder as? ViewHolderCoin)?.bind(item)
        }
    }

    companion object {
        val coinRateDiff = object: DiffUtil.ItemCallback<ViewItem>() {
            override fun areItemsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}


class ViewHolderSource(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

class ViewHolderCoin(override val containerView: View, listener: CoinRatesAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var coinViewItem: ViewItem.CoinViewItem? = null

    init {
        containerView.setOnClickListener {
            coinViewItem?.let {
                listener.onCoinClicked(it)
            }
        }
    }

    fun bind(viewItem: ViewItem.CoinViewItem) {
        this.coinViewItem = viewItem

        coinIcon.isVisible = viewItem.coinItem.coin != null
        viewItem.coinItem.coin?.code?.let { coinIcon.setCoinImage(it) }
        titleText.text = viewItem.coinItem.coinName
        subtitleText.text = viewItem.coinItem.coinCode

        txValueInFiat.isActivated = !viewItem.coinItem.rateDimmed //change color via state: activated/not activated
        txValueInFiat.text = viewItem.coinItem.rate ?: containerView.context.getString(R.string.NotAvailable)

        if (viewItem.coinItem.diff != null) {
            txDiff.diff = viewItem.coinItem.diff
            txDiff.visibility = View.VISIBLE
            txDiffNa.visibility = View.GONE
        } else {
            txDiff.visibility = View.GONE
            txDiffNa.visibility = View.VISIBLE
        }

        bottomShade.visibility = if (viewItem.last) View.VISIBLE else View.GONE
    }
}
