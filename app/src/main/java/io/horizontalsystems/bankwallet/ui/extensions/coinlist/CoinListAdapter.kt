package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setImage
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.ViewHolderCoinManageItemBinding
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.views.ListPosition

class CoinListAdapter(private val listener: Listener) :
    ListAdapter<CoinViewItem, CoinWithSwitchViewHolder>(diffCallback) {

    interface Listener {
        fun enable(uid: String)
        fun disable(uid: String)
        fun edit(uid: String) = Unit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinWithSwitchViewHolder {
        return CoinWithSwitchViewHolder(
            ViewHolderCoinManageItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            { isChecked, index ->
                val item = getItem(index)
                onSwitchToggle(isChecked, item.uid)
            }
        ) { index ->
            val item = getItem(index)
            listener.edit(item.uid)
        }
    }

    override fun onBindViewHolder(holder: CoinWithSwitchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun onSwitchToggle(isChecked: Boolean, uid: String) {
        if (isChecked) {
            listener.enable(uid)
        } else {
            listener.disable(uid)
        }
    }

    fun disableCoin(uid: String): Boolean {
        for (i in 0 until itemCount) {
            if (getItem(i).uid == uid) {
                notifyItemChanged(i)
                return true
            }
        }
        return false
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<CoinViewItem>() {
            override fun areItemsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem.uid == newItem.uid
            }

            override fun areContentsTheSame(oldItem: CoinViewItem, newItem: CoinViewItem): Boolean {
                return oldItem.title == newItem.title && oldItem.subtitle == newItem.subtitle && oldItem.state == newItem.state && oldItem.listPosition == newItem.listPosition
            }
        }
    }

}

class CoinWithSwitchViewHolder(
    private val binding: ViewHolderCoinManageItemBinding,
    private val onSwitch: (isChecked: Boolean, position: Int) -> Unit,
    private val onEdit: (position: Int) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.wrapper.setOnClickListener {
            binding.toggleSwitch.isChecked = !binding.toggleSwitch.isChecked
        }
    }

    fun bind(viewItem: CoinViewItem) {
        set(viewItem, viewItem.listPosition)

        when (viewItem.state) {
            CoinViewItemState.ToggleHidden -> {
                binding.toggleSwitch.setOnCheckedChangeListener { _, _ -> }
                binding.toggleSwitch.isVisible = false
                binding.edit.isVisible = false
            }
            is CoinViewItemState.ToggleVisible -> {
                // set switch value without triggering onChangeListener
                binding.toggleSwitch.setOnCheckedChangeListener(null)
                binding.toggleSwitch.isChecked = viewItem.state.enabled
                binding.toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSwitch(isChecked, bindingAdapterPosition)
                }
                binding.toggleSwitch.isVisible = true

                binding.edit.isVisible = viewItem.state.hasSettings
                binding.edit.setOnSingleClickListener {
                    onEdit(bindingAdapterPosition)
                }
            }
        }
    }

    private fun set(coinViewItem: CoinViewItem, listPosition: ListPosition) {
        binding.backgroundView.setBackgroundResource(getBackground(listPosition))
        binding.coinIcon.setImage(coinViewItem.imageSource)
        binding.coinTitle.text = coinViewItem.title
        binding.coinSubtitle.text = coinViewItem.subtitle
        binding.dividerView.isVisible =
            listPosition == ListPosition.Last || listPosition == ListPosition.Single
    }

    private fun getBackground(listPosition: ListPosition): Int {
        return when (listPosition) {
            ListPosition.First -> R.drawable.border_steel10_top
            ListPosition.Middle -> R.drawable.border_steel10_top
            ListPosition.Last -> R.drawable.border_steel10_top_bottom
            ListPosition.Single -> R.drawable.border_steel10_top_bottom
        }
    }

}

data class CoinViewItem(
    val uid: String,
    val imageSource: ImageSource,
    val title: String,
    val subtitle: String,
    val state: CoinViewItemState,
    val listPosition: ListPosition
)

sealed class CoinViewItemState {
    data class ToggleVisible(val enabled: Boolean, val hasSettings: Boolean) : CoinViewItemState()
    object ToggleHidden : CoinViewItemState()
}
