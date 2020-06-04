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

class CoinRatesAdapter(private val listener: Listener) : ListAdapter<CoinItem, ViewHolderCoin>(coinRateDiff) {

    interface Listener {
        fun onCoinClicked(coinItem: CoinItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCoin {
        return ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_rate, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderCoin, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val coinRateDiff = object: DiffUtil.ItemCallback<CoinItem>() {
            override fun areItemsTheSame(oldItem: CoinItem, newItem: CoinItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CoinItem, newItem: CoinItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class ViewHolderCoin(override val containerView: View, listener: CoinRatesAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var coinItem: CoinItem? = null

    init {
        containerView.setOnClickListener {
            coinItem?.let {
                listener.onCoinClicked(it)
            }
        }
    }

    fun bind(coinItem: CoinItem) {
        this.coinItem = coinItem

        coinIcon.isVisible = coinItem.coin != null
        coinItem.coin?.code?.let { coinIcon.setCoinImage(it) }
        titleText.text = coinItem.coinName
        subtitleText.text = coinItem.coinCode

        txValueInFiat.isActivated = !coinItem.rateDimmed //change color via state: activated/not activated
        txValueInFiat.text = coinItem.rate ?: containerView.context.getString(R.string.NotAvailable)

        if (coinItem.diff != null) {
            txDiff.diff = coinItem.diff
            txDiff.visibility = View.VISIBLE
            txDiffNa.visibility = View.GONE
        } else {
            txDiff.visibility = View.GONE
            txDiffNa.visibility = View.VISIBLE
        }
    }
}
