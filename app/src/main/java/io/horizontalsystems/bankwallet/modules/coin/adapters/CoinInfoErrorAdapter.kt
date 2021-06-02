package io.horizontalsystems.bankwallet.modules.coin.adapters

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
import kotlinx.android.synthetic.main.view_holder_coin_error.*

class CoinInfoErrorAdapter(
        showErrorLiveData: LiveData<String>,
        viewLifecycleOwner: LifecycleOwner
) : ListAdapter<String, CoinInfoErrorAdapter.ViewHolder>(diff) {

    init {
        showErrorLiveData.observe(viewLifecycleOwner, { error ->
            if (error.isNotEmpty()){
                submitList(listOf(error))
            } else {
                submitList(listOf())
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(error: String) {
            errorText.text = error
        }

        companion object {
            fun create(parent: ViewGroup): ViewHolder {
                return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin_error, parent, false))
            }
        }
    }

}
