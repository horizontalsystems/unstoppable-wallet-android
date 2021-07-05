package io.horizontalsystems.bankwallet.modules.coin.tvlrank

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer

class TvlRankLoadingAdapter(
    loadingLiveData: LiveData<Boolean>,
    viewLifecycleOwner: LifecycleOwner
) : ListAdapter<Boolean, ViewHolderCoinLoading>(diff) {

    init {
        loadingLiveData.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                submitList(listOf(loading))
            } else {
                submitList(listOf())
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderCoinLoading {
        return ViewHolderCoinLoading.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolderCoinLoading, position: Int) = Unit

    companion object {
        private val diff = object : DiffUtil.ItemCallback<Boolean>() {
            override fun areItemsTheSame(oldItem: Boolean, newItem: Boolean): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Boolean, newItem: Boolean): Boolean {
                return oldItem == newItem
            }
        }
    }

}

class ViewHolderCoinLoading(override val containerView: View) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {
    companion object {
        fun create(parent: ViewGroup): ViewHolderCoinLoading {
            return ViewHolderCoinLoading(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_holder_tvlrank_loading, parent, false)
            )
        }
    }
}
