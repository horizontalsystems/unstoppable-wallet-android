package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_subtitle.*
import java.math.BigDecimal
import java.util.*

class CoinSubtitleAdapter(
        viewItemLiveData: MutableLiveData<ViewItemWrapper>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<CoinSubtitleAdapter.ViewItemWrapper, CoinSubtitleAdapter.ViewHolder>(diff) {

    init {
        viewItemLiveData.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflate(parent, R.layout.view_holder_coin_subtitle, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        val prev = payloads.lastOrNull() as? ViewItemWrapper

        if (prev == null) {
            holder.bind(item)
        } else {
            holder.bindUpdate(item, prev)
        }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<ViewItemWrapper>() {
            override fun areItemsTheSame(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Boolean = true

            override fun areContentsTheSame(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Boolean {
                return oldItem.rate == newItem.rate && oldItem.rateDiff == newItem.rateDiff
            }

            override fun getChangePayload(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Any? {
                return oldItem
            }
        }
    }

    data class ViewItemWrapper(
            val coinName: String,
            val coinType: CoinType,
            val rating: String?,
            val rate: String?,
            val rateDiff: BigDecimal?,
            val grade: String?
    )

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(item: ViewItemWrapper) {
            coinIcon.setCoinImage(item.coinType)
            coinNameTextView.text = item.coinName
            coinRateLast.text = item.rate
            coinRateDiff.setDiff(item.rateDiff)

            // Coin Rating
            coinRating.isVisible = !item.rating.isNullOrBlank()
            coinRating.setImageDrawable(getRatingIcon(item.rating))
            coinRating.isEnabled = false
        }

        private fun getRatingIcon(rating: String?): Drawable? {
            val icon = when (rating?.toLowerCase(Locale.ENGLISH)) {
                "a" -> R.drawable.ic_rating_a
                "b" -> R.drawable.ic_rating_b
                "c" -> R.drawable.ic_rating_c
                "d" -> R.drawable.ic_rating_d
                else -> return null
            }
            return ContextCompat.getDrawable(containerView.context, icon)
        }

        fun bindUpdate(current: ViewItemWrapper, prev: ViewItemWrapper) {
            current.apply {
                if (coinType != prev.coinType){
                    coinIcon.setCoinImage(coinType)
                }
                if (coinName != prev.coinName) {
                    coinNameTextView.text = coinName
                }
                if (rate != prev.rate) {
                    coinRateLast.text = rate
                }
                if (rateDiff != prev.rateDiff) {
                    coinRateDiff.setDiff(rateDiff)
                }
                if (rating != prev.rating) {
                    coinRating.isVisible = !rating.isNullOrBlank()
                    coinRating.setImageDrawable(getRatingIcon(rating))
                    coinRating.isEnabled = false
                }

            }
        }

    }
}
