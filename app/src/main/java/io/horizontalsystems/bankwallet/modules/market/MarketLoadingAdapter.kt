package io.horizontalsystems.bankwallet.modules.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_loading.*

class MarketLoadingAdapter(
        loadingLiveData: LiveData<Boolean>,
        errorLiveData: LiveData<String?>,
        private val onErrorClick: () -> Unit,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<MarketLoadingState, ViewHolderMarketLoading>(marketStateDiff) {

    private var loading = false
    private var error: String? = null

    init {
        loadingLiveData.observe(viewLifecycleOwner, {
            loading = it
            syncState()
        })
        errorLiveData.observe(viewLifecycleOwner, {
            error = it
            syncState()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketLoading {
        return ViewHolderMarketLoading.create(parent, onErrorClick)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketLoading, position: Int) {
        holder.bind(getItem(position))
    }

    @Synchronized
    private fun syncState() {
        val newItems = when {
            loading -> listOf(MarketLoadingState.Loading)
            error != null -> listOf(MarketLoadingState.Error(R.string.BalanceSyncError_Title))
            else -> listOf()
        }
        submitList(newItems)
    }

    companion object {
        private val marketStateDiff = object : DiffUtil.ItemCallback<MarketLoadingState>() {
            override fun areItemsTheSame(oldItem: MarketLoadingState, newItem: MarketLoadingState): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: MarketLoadingState, newItem: MarketLoadingState): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: MarketLoadingState, newItem: MarketLoadingState): Any? {
                return oldItem
            }
        }
    }

}

sealed class MarketLoadingState {
    object Loading : MarketLoadingState()
    class Error(@StringRes val message: Int) : MarketLoadingState()

    override fun equals(other: Any?): Boolean {
        return this is Loading && other is Loading ||
                this is Error && other is Error && message == other.message
    }

    override fun hashCode(): Int {
        return when (this) {
            is Loading -> Loading.javaClass.hashCode()
            is Error -> message.hashCode()
        }
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

class ViewHolderMarketLoading(override val containerView: View, onErrorClick: () -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var marketLoadingState: MarketLoadingState? = null

    init {
        containerView.setOnClickListener {
            if (marketLoadingState is MarketLoadingState.Error) {
                onErrorClick()
            }
        }
    }

    fun bind(item: MarketLoadingState) {
        marketLoadingState = item

        when (item) {
            MarketLoadingState.Loading -> {
                progressBar.isVisible = true
                error.isVisible = false
            }
            is MarketLoadingState.Error -> {
                progressBar.isVisible = false

                error.isVisible = true
                error.setText(item.message)
            }
        }
    }

    companion object {
        fun create(parent: ViewGroup, onErrorClick: () -> Unit): ViewHolderMarketLoading {
            return ViewHolderMarketLoading(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_loading, parent, false), onErrorClick)
        }
    }

}