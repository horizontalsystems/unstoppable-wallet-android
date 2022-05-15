package io.horizontalsystems.bankwallet.modules.market

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewHolderMarketLoadingBinding

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
        return ViewHolderMarketLoading(
            ViewHolderMarketLoadingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onErrorClick
        )
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
            override fun areItemsTheSame(
                oldItem: MarketLoadingState,
                newItem: MarketLoadingState
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: MarketLoadingState,
                newItem: MarketLoadingState
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: MarketLoadingState,
                newItem: MarketLoadingState
            ): Any? {
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

class ViewHolderMarketLoading(
    private val binding: ViewHolderMarketLoadingBinding,
    onErrorClick: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var marketLoadingState: MarketLoadingState? = null

    init {
        binding.wrapper.setOnClickListener {
            if (marketLoadingState is MarketLoadingState.Error) {
                onErrorClick()
            }
        }
    }

    fun bind(item: MarketLoadingState) {
        marketLoadingState = item

        when (item) {
            MarketLoadingState.Loading -> {
                binding.progressBar.isVisible = true
                binding.error.isVisible = false
            }
            is MarketLoadingState.Error -> {
                binding.progressBar.isVisible = false

                binding.error.isVisible = true
                binding.error.setText(item.message)
            }
        }
    }

}
