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

class CoinRatesAdapter(private val listener: Listener) : ListAdapter<CoinViewItem, ViewHolderCoin>(coinRateDiff) {

    interface Listener {
        fun onCoinClicked(coinViewItem: CoinViewItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCoin {
        return ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderCoin, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val coinRateDiff = object: DiffUtil.ItemCallback<CoinViewItem>() {
            override fun areItemsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class ViewHolderCoin(override val containerView: View, listener: CoinRatesAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var coinViewItem: CoinViewItem? = null

    init {
        containerView.setOnClickListener {
            coinViewItem?.let {
                listener.onCoinClicked(it)
            }
        }
    }

    fun bind(viewItem: CoinViewItem) {
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
