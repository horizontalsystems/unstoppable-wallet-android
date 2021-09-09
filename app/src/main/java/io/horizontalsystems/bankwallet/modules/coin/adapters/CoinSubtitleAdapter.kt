package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_subtitle.*
import java.math.BigDecimal

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

            setRatingIcon(item.rating)
        }

        private fun setRatingIcon(rating: String?) {
            coinRatingCompose.setContent {
                if(!rating.isNullOrBlank()){
                    ComposeAppTheme {
                        ButtonSecondaryCircle(
                            icon = getRatingIcon(rating),
                            onClick = { },
                            enabled = false,
                            tint = getTintColor(rating)
                        )
                    }
                }
            }
        }

        private fun getRatingIcon(rating: String): Int {
            return when (rating.lowercase()) {
                "a" -> R.drawable.ic_rating_a
                "b" -> R.drawable.ic_rating_b
                "c" -> R.drawable.ic_rating_c
                "d" -> R.drawable.ic_rating_d
                else -> throw IllegalArgumentException("Argument supplied: $rating")
            }
        }

        private fun getTintColor(rating: String): Color {
            return when (rating.lowercase()) {
                "a" -> Color(0xFFFFA800)
                "b" -> Color(0xFF3372FF)
                "c" -> Color(0xFF808085)
                "d" -> Color(0xFFC8C7CC)
                else -> throw IllegalArgumentException("Argument supplied: $rating")
            }
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
                    setRatingIcon(rating)
                }
            }
        }

    }
}
