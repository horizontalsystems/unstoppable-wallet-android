package io.horizontalsystems.bankwallet.modules.cryptonews

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_crypto_news_title.*

class CryptoNewsHeaderAdapter : RecyclerView.Adapter<CryptoNewsHeaderAdapter.TitleViewHolder>() {
    var loading = false
    var notAvailable = false

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        return TitleViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        holder.bind(loading, notAvailable)
    }

    fun bind(loading: Boolean, notAvailable: Boolean) {
        this.loading = loading
        this.notAvailable = notAvailable
        notifyItemChanged(0)
    }

    class TitleViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(loading: Boolean, notAvailable: Boolean) {
            this.spinner.isVisible = loading
            this.notAvailable.isVisible = notAvailable
        }

        companion object {
            const val layout = R.layout.view_holder_crypto_news_title

            fun create(parent: ViewGroup) = TitleViewHolder(inflate(parent, layout, false))
        }

    }
}
