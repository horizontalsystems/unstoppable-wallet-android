package io.horizontalsystems.bankwallet.modules.market.favorites

import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class EmptyListAdapter(
        showEmptyListText: LiveData<Boolean>,
        viewLifecycleOwner: LifecycleOwner,
        private val viewHolderFactoryMethod: (parent: ViewGroup, viewType: Int) -> RecyclerView.ViewHolder
) : ListAdapter<Boolean, RecyclerView.ViewHolder>(diffCallback) {

    init {
        showEmptyListText.observe(viewLifecycleOwner, { show ->
            submitList(if (show) listOf(true) else listOf())
        })
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactoryMethod(parent, viewType)
    }

    companion object{
        private val diffCallback = object : DiffUtil.ItemCallback<Boolean>() {
            override fun areItemsTheSame(oldItem: Boolean, newItem: Boolean): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Boolean, newItem: Boolean): Boolean {
                return oldItem == newItem
            }
        }
    }

}

class SpacerAdapter : RecyclerView.Adapter<SpacerViewHolder>() {
    var show = false
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpacerViewHolder {
        return SpacerViewHolder(ComposeView(parent.context))
    }

    override fun onViewRecycled(holder: SpacerViewHolder) {
        holder.composeView.disposeComposition()
    }

    override fun onBindViewHolder(holder: SpacerViewHolder, position: Int) {
        holder.bind(show)
    }

    override fun getItemCount() = 1
}

class SpacerViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }

    fun bind(show: Boolean) {
        composeView.setContent {
            ComposeAppTheme {
                if (show) {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
