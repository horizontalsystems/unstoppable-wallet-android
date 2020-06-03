package io.horizontalsystems.bankwallet.modules.cryptonews

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import io.horizontalsystems.xrateskit.entities.CryptoNews

class CryptoNewsAdapter : ListAdapter<CryptoNews, ViewHolderNews>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderNews {
        return ViewHolderNews(inflate(parent, R.layout.view_holder_crypto_news))
    }

    override fun onBindViewHolder(holder: ViewHolderNews, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CryptoNews>() {
            override fun areItemsTheSame(oldItem: CryptoNews, newItem: CryptoNews): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: CryptoNews, newItem: CryptoNews): Boolean {
                return oldItem == newItem
            }
        }
    }
}
